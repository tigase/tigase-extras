/*
 * MDnsComponent.java
 *
 * Tigase Jabber/XMPP Server - Extras
 * Copyright (C) 2004-2017 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */

import tigase.conf.ConfigurationException;
import tigase.http.AbstractHttpServer;
import tigase.http.HttpMessageReceiver;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.RegistrarBean;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.core.Kernel;
import tigase.net.SocketType;
import tigase.server.AbstractComponentRegistrator;
import tigase.server.ConnectionManager;
import tigase.server.ServerComponent;
import tigase.server.bosh.BoshConnectionManager;
import tigase.server.websocket.WebSocketClientConnectionManager;
import tigase.server.xmppclient.ClientConnectionManager;
import tigase.server.xmppserver.S2SConnectionManager;
import tigase.sys.ShutdownHook;
import tigase.sys.TigaseRuntime;
import tigase.util.dns.DNSResolverFactory;

import javax.jmdns.JmDNS;
import javax.jmdns.NetworkTopologyDiscovery;
import javax.jmdns.ServiceInfo;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Bean(name = "mdns", parent = Kernel.class, active = false)
public class MDnsComponent
		extends AbstractComponentRegistrator
		implements RegistrarBean, Initializable, UnregisterAware, ShutdownHook {

	private static final Logger log = Logger.getLogger(MDnsComponent.class.getCanonicalName());
	private final ConcurrentHashMap<InetAddress, JmDNSItem> instances = new ConcurrentHashMap<>();
	private Kernel kernel;
	@ConfigField(desc = "Advertised hostname of the server", alias = "server-host")
	private String serverHost;
	@ConfigField(desc = "Advertised server name", alias = "server-name")
	private String serverName = "Tigase XMPP Server";
	private Map<String, List<ServiceInfo>> servicesPerComponent = new ConcurrentHashMap<>();
	@ConfigField(desc = "Force single server for domain", alias = "single-server")
	private boolean singleServer = false;
	@ConfigField(desc = "Ignore link-local addresses", alias = "ignore-link-local")
	private boolean ignoreLinkLocal = true;
	private Timer timer;

	public MDnsComponent() {
		serverHost = DNSResolverFactory.getInstance().getDefaultHost();
		int idx = serverHost.indexOf('.');
		if (idx > 0) {
			serverHost = serverHost.substring(0, idx);
		}
	}

	@Override
	public String getDiscoCategoryType() {
		return super.getDiscoCategoryType();
	}

	@Override
	public String getDiscoDescription() {
		return "mDNS component";
	}

	@Override
	public synchronized void componentAdded(ServerComponent component) throws ConfigurationException {
		if (component instanceof S2SConnectionManager) {
			forEachConnection((ConnectionManager) component, (socketType, port) -> {
				addService(component.getName(), "_xmpp-server._tcp.local", port, "S2S");
			});
		} else if (component instanceof WebSocketClientConnectionManager) {
			forEachConnection((ConnectionManager) component, (socketType, port) -> {
				addService(component.getName(), "_xmppconnect.local",
						   "_xmpp-client-websocket=" + (socketType == SocketType.plain ? "ws" : "wss") + "://" +
								   serverHost + ":" + port + "/");
			});
		} else if (component instanceof BoshConnectionManager) {
			forEachConnection((ConnectionManager) component, (socketType, port) -> {
				addService(component.getName(), "_xmppconnect",
						   "_xmpp-client-xbosh=" + (socketType == SocketType.plain ? "http" : "https") + "://" +
								   serverHost + ":" + port + "/");
			});
		} else if (component instanceof ClientConnectionManager) {
			forEachConnection((ConnectionManager) component, (socketType, port) -> {
				if (socketType == SocketType.plain) {
					addService(component.getName(), "_xmpp-client._tcp.local", port, "C2S");
				} else {
					addService(component.getName(), "_xmpps-client._tcp.local", port, "C2S over TLS");
				}
			});
		} else if (component instanceof HttpMessageReceiver) {
			forEachConnection((HttpMessageReceiver) component, ((socketType, port) -> {
				if (socketType == SocketType.plain) {
					addService(component.getName(), "_http._tcp.local", port, "HTTP Server");
				} else {
					addService(component.getName(), "_https._tcp.local", port, "HTTPS Server");
				}
			}));
		}
	}

	@Override
	public synchronized void componentRemoved(ServerComponent component) {
		removeServices(component.getName());
	}

	@Override
	public boolean isCorrectType(ServerComponent component) {
		return component instanceof ClientConnectionManager || component instanceof S2SConnectionManager ||
				component instanceof HttpMessageReceiver;
	}

	@Override
	public void register(Kernel kernel) {
		this.kernel = kernel;
	}

	@Override
	public void unregister(Kernel kernel) {
		this.kernel = null;
	}

	@Override
	public void initialize() {
		super.initialize();
		if (singleServer) {
			ensureSingleServer();
		}
		timer = new Timer(true);
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				updateNetworkTopology();
			}
		}, 1, 10 * 1000);
		TigaseRuntime.getTigaseRuntime().addShutdownHook(this);
	}

	private void updateNetworkTopology() {
		try {
			Set<InetAddress> addresses = new HashSet(Arrays.asList(NetworkTopologyDiscovery.Factory.getInstance().getInetAddresses()));
			addresses.stream().filter(inetAddress -> !ignoreLinkLocal || !inetAddress.isLinkLocalAddress()).forEach(addr -> {
				JmDNSItem jmDNS = instances.computeIfAbsent(addr, this::createJmDNSItem);
			});
			List<InetAddress> toRemove = instances.keySet().stream().filter(addr -> !addresses.contains(addr)).collect(Collectors.toList());
			for (InetAddress addr : toRemove) {
				if (log.isLoggable(Level.FINEST)) {
					log.finest("stopping JmDNS for " + addr);
				}
				stopJmDNS(instances.remove(addr));
			}
		} catch (Throwable ex) {
			log.log(Level.SEVERE, "got exception during network topology updated", ex);
		}
	}

	private JmDNSItem createJmDNSItem(InetAddress addr) {
		try {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("starting JmDNS for " + addr);
			}
			JmDNSItem item =  new JmDNSItem(addr, serverHost);
			initializeJmDNS(item);
			return item;
		} catch (IOException ex) {
			return null;
		}
	}

	private void stopJmDNS(JmDNSItem jmDNS) {
		if (jmDNS != null) {
			try {
				jmDNS.close();
			} catch (IOException ex) {
				log.log(Level.WARNING, "failed to stop mDNS service", ex);
			}
		}
	}

	private void initializeJmDNS(JmDNSItem jmDNSItem) {
		servicesPerComponent.values().stream().flatMap(List::stream).forEach(info -> {
				jmDNSItem.execute(jmDNS -> {
					try {
						if (log.isLoggable(Level.FINEST)) {
							log.finest("at " + jmDNS.getInetAddress() + " registering " + info.toString());
						}
						jmDNS.registerService(info.clone());
					} catch (IOException ex) {
						log.log(Level.WARNING, "Could not advertise mDNS records = " + info.getNiceTextString(), ex);
					}
				});
		});
	}

	@Override
	public void beforeUnregister() {
		shutdown();
		TigaseRuntime.getTigaseRuntime().removeShutdownHook(this);
	}

	@Override
	public String shutdown() {
		timer.cancel();
		instances.values().stream().forEach(this::stopJmDNS);
		return null;
	}

	private void addService(String compName, String type, int port, String suffix) {
		ServiceInfo info = ServiceInfo.create(type, serverHost, port, serverName + " - " + suffix);
		addService(compName, info);
	}

	private void addService(String compName, String type, String suffix) {
		ServiceInfo info = ServiceInfo.create(type, serverHost, 0, suffix);
		addService(compName, info);
	}

	private void addService(String compName, ServiceInfo info) {
			List<ServiceInfo> services = servicesPerComponent.computeIfAbsent(compName, (k) -> new ArrayList<>());
			services.add(info);
			forEachJmDNS(jmDNS -> {
				try {
					jmDNS.registerService(info.clone());
				} catch (IOException ex) {
					log.log(Level.WARNING, "Could not advertise mDNS records = " + info.getNiceTextString(), ex);
				}
			});
	}

	private void removeServices(String compName) {
		List<ServiceInfo> services = servicesPerComponent.remove(compName);
		if (services != null) {
			services.forEach(info -> forEachJmDNS(jmDNS -> jmDNS.unregisterService(info)));
		}
	}

	private void forEachJmDNS(Consumer<JmDNS> task) {
		instances.values().forEach(item -> item.execute(task));
	}

	private void forEachConnection(ConnectionManager component, BiConsumer<SocketType, Integer> consumer) {
		try {
			Kernel compKernel = kernel.getInstanceIfExistsOr(component.getName() + "#KERNEL", (bc) -> null);
			ConnectionManager.PortsConfigBean portsBean = compKernel.getInstanceIfExistsOr("connections", (bc) -> null);
			Field f = ConnectionManager.PortsConfigBean.class.getDeclaredField("portsBeans");
			f.setAccessible(true);
			ConnectionManager.PortConfigBean[] portBeans = (ConnectionManager.PortConfigBean[]) f.get(portsBean);
			if (portBeans != null) {
				for (ConnectionManager.PortConfigBean portConfigBean : portBeans) {
					Field nameField = ConnectionManager.PortConfigBean.class.getDeclaredField("name");
					nameField.setAccessible(true);
					Field socketField = ConnectionManager.PortConfigBean.class.getDeclaredField("socket");
					socketField.setAccessible(true);
					consumer.accept((SocketType) socketField.get(portConfigBean),
									(Integer) nameField.get(portConfigBean));
				}
			}
		} catch (Exception ex) {
			log.log(Level.WARNING, "Could not retrieve opened ports for component " + component.getName(), ex);
		}
	}

	private void forEachConnection(HttpMessageReceiver component, BiConsumer<SocketType, Integer> consumer) {
		try {
			Kernel compKernel = kernel.getInstanceIfExistsOr("httpServer#KERNEL", (bc) -> null);
			AbstractHttpServer.PortsConfigBean portsBean = compKernel.getInstanceIfExistsOr("connections",
																							(bc) -> null);
			Field f = AbstractHttpServer.PortsConfigBean.class.getDeclaredField("portsBeans");
			f.setAccessible(true);
			AbstractHttpServer.PortConfigBean[] portBeans = (AbstractHttpServer.PortConfigBean[]) f.get(portsBean);
			if (portBeans != null) {
				for (AbstractHttpServer.PortConfigBean portConfigBean : portBeans) {
					consumer.accept(portConfigBean.getSocket(), portConfigBean.getPort());
				}
			}
		} catch (Exception ex) {
			log.log(Level.WARNING, "Could not retrieve opened ports for component httpServer", ex);
		}
	}

	private void ensureSingleServer() {
		try {
			HashSet<InetAddress> localAddresses = new HashSet<>(Arrays.asList(NetworkTopologyDiscovery.Factory.getInstance().getInetAddresses()));
			List<InetAddress> nonlocalAddresses = Arrays.stream(InetAddress.getAllByName(serverHost))
					.filter(address -> !localAddresses.contains(address))
					.collect(Collectors.toList());

			if (!nonlocalAddresses.isEmpty() && !dnsInjectionWorkaround(nonlocalAddresses)) {
				TigaseRuntime.getTigaseRuntime()
						.shutdownTigase(new String[]{"Error! Terminating the server process.",
													 "Local mDNS domain " + serverHost +
															 " points to non-local IP addresses:",
													 nonlocalAddresses.stream()
															 .map(InetAddress::getHostAddress).collect(
															 Collectors.joining(", "))});
			}
		} catch (UnknownHostException ex) {
			// this is expected!
		}
	}

	private boolean dnsInjectionWorkaround(List<InetAddress> nonlocalAddresses) {
		if (!nonlocalAddresses.stream().anyMatch(InetAddress::isSiteLocalAddress)) {
			try {
				if (Arrays.stream(InetAddress.getAllByName("tig-" + UUID.randomUUID() + "-hub"))
						.anyMatch(nonlocalAddresses::contains)) {
					log.warning("non-existing domain resolved to the same address as " + serverHost +
										", disabling forcing single server for domain!!");
					return true;
				}
			} catch (UnknownHostException ex) {
				// this is expected
			}
		}
		return false;
	}

	private class JmDNSItem implements JmDNS.Delegate {

		private final InetAddress address;
		private final String name;
		public JmDNS jmDNS;
		private ExecutorService executor;
		private final LinkedList<Runnable> awaiting = new LinkedList<>();
		private boolean stopped = false;

		public JmDNSItem(InetAddress address, String name) throws IOException {
			this.address = address;
			this.name = name;
			this.jmDNS = JmDNS.create(address, name);
			this.jmDNS.setDelegate(this::cannotRecoverFromIOError);
			this.executor = Executors.newSingleThreadExecutor();
		}

		public synchronized void execute(Consumer<JmDNS> task) {
			Runnable run = () -> executeTask(task);
			try {
				executor.execute(run);
			} catch (RejectedExecutionException ex) {
				awaiting.offer(run);
			}
		}

		public synchronized void close() throws IOException {
			stopped = true;
			jmDNS.setDelegate(null);
			executor.shutdownNow();
			jmDNS.close();
		}

		@Override
		public synchronized void cannotRecoverFromIOError(JmDNS jmDNS, Collection<ServiceInfo> serviceInfos) {
			jmDNS.setDelegate(null);
			try {
				jmDNS.close();
			} catch (Exception ex) {}
			if (stopped) {
				return;
			}
			final List<Runnable> cancelled = executor.shutdownNow();
			MDnsComponent.this.timer.schedule(new TimerTask() {
				@Override
				public void run() {
					restartJmDNS(cancelled, serviceInfos);
				}
			}, 10 * 1000);
		}

		private synchronized void restartJmDNS(List<Runnable> cancelled, Collection<ServiceInfo> serviceInfos) {
			try {
				jmDNS.setDelegate(null);
				if (stopped) {
					return;
				}
				try {
					jmDNS = JmDNS.create(address, name);
					jmDNS.setDelegate(this);
					serviceInfos.forEach(info -> {
						try {
							jmDNS.registerService(info.clone());
						} catch (IOException ex) {
							log.log(Level.WARNING, "Could not advertise mDNS records = " + info.getNiceTextString(), ex);
						}
					});
					executor = Executors.newSingleThreadExecutor();
					cancelled.forEach(executor::submit);
					Runnable task = null;
					while ((task = awaiting.poll()) != null) {
						executor.submit(task);
					}
				} catch (IOException ex) {
					jmDNS.close();
					MDnsComponent.this.timer.schedule(new TimerTask() {
						@Override
						public void run() {
							restartJmDNS(cancelled, serviceInfos);
						}
					}, 10 * 1000);
				}
			} catch (Throwable ex) {
				log.log(Level.SEVERE, "failed to restart JmDNS", ex);
			}
		}

		private void executeTask(Consumer<JmDNS> task) {
			task.accept(jmDNS);
		}
	}
}
