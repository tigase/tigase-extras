/*
 * Tigase Server Extras MongoDB - Extra modules to Tigase Server
 * Copyright (C) 2007 Tigase, Inc. (office@tigase.com)
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
package tigase.ldap;

import com.unboundid.ldap.protocol.ExtendedRequestProtocolOp;
import com.unboundid.ldap.protocol.ExtendedResponseProtocolOp;
import com.unboundid.ldap.protocol.LDAPMessage;
import com.unboundid.ldap.protocol.ProtocolOp;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.extensions.StartTLSExtendedRequest;
import tigase.auth.TigaseSaslProvider;
import tigase.db.AuthRepository;
import tigase.db.UserRepository;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.beans.selector.ConfigType;
import tigase.kernel.beans.selector.ConfigTypeEnum;
import tigase.kernel.core.Kernel;
import tigase.ldap.processors.LDAPProcessor;
import tigase.ldap.processors.LDAPSession;
import tigase.net.IOService;
import tigase.net.SocketThread;
import tigase.server.Packet;
import tigase.socks5.AbstractConnectionManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

@Bean(name = "ldap", parent = Kernel.class, active = false)
@ConfigType(ConfigTypeEnum.DefaultMode)
public class LdapConnectionManager extends AbstractConnectionManager<LdapIOService<Object>> {

	private static final Logger log = Logger.getLogger(LdapConnectionManager.class.getName());

	@Inject
	private AuthRepository authRepository;
	@Inject
	private UserRepository userRepository;
	
	@Inject
	private List<LDAPProcessor> processors = new ArrayList<>();

	private Executor executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

	@ConfigField(desc = "Allow anonymous access")
	private boolean anonymousAccess = false;
	@ConfigField(desc = "Anyone can query data of all users")
	private boolean anyoneCanQuery = false;

	public boolean isAnonymousAccessAllowed() {
		return anonymousAccess;
	}

	public boolean isAnyoneCanQuery() {
		return anyoneCanQuery;
	}

	@Override
	public void serviceStarted(LdapIOService<Object> serv) {
		super.serviceStarted(serv);
		serv.getSessionData().put(IOService.HOSTNAME_KEY, "localhost");
		serv.setConnectionManager(this);
	}

	@Override
	public void packetsReady(LdapIOService<Object> serv) throws IOException {
		LDAPMessage message;
		Queue<LDAPMessage> requests = serv.getReceivedRequests();
		while ((message = requests.poll()) != null) {
			processRequest(serv, message);
		}
	}

	protected void processRequest(LdapIOService<Object> serv, LDAPMessage message) throws IOException {
		executor.execute(() -> {
			try {
				if (message.getProtocolOp() instanceof ExtendedRequestProtocolOp) {
					ExtendedRequestProtocolOp extendedOp = ((ExtendedRequestProtocolOp) message.getProtocolOp());
					if (extendedOp.getOID().equals(StartTLSExtendedRequest.STARTTLS_REQUEST_OID)) {
						SocketThread.removeSocketService(serv);
						serv.sendResponse(new LDAPMessage(message.getMessageID(),
														  new ExtendedResponseProtocolOp(ResultCode.SUCCESS_INT_VALUE,
																						 null, null, null, null,
																						 null)));
						serv.processWaitingPackets();
						serv.startTLS(false, false, false);
						SocketThread.addSocketService(serv);
						return;
					}
				}

				int msgId = message.getMessageID();
				processRequest(serv, message.getProtocolOp(), response -> {
					serv.sendResponse(new LDAPMessage(msgId, response));
					if (serv.writeInProgress.tryLock()) {
						try {
							serv.processWaitingPackets();
							SocketThread.addSocketService(serv);
						} catch (Exception e) {
							log.log(Level.WARNING, "Exception during writing packets [" + serv + "[: ", e);
							try {
								serv.forceStop();
							} catch (Exception e1) {
								log.log(Level.WARNING, "Exception stopping XMPPIOService [" + serv + "]: ", e1);
							}    // end of try-catch
						} finally {
							serv.writeInProgress.unlock();
						}
					}
				});
			} catch (Throwable ex) {
				log.log(Level.SEVERE, ex.getMessage(), ex);
			}
		});
	}
	
	protected void processRequest(LDAPSession session, ProtocolOp request, Consumer<ProtocolOp> consumer) throws Throwable {
		log.finest(() -> "processing LDAP request: " + request);
		for (LDAPProcessor processor : processors) {
			if (processor.canHandle(request)) {
				try {
					processor.process(session, request, consumer);
				} catch (Throwable ex) {
					log.log(Level.SEVERE, ex.getMessage(), ex);
				}
				return;
			}
		}
		consumer.accept(new ExtendedResponseProtocolOp(ResultCode.NOT_SUPPORTED_INT_VALUE, null, null, null, null, null));
	}
	
	@Override
	public String getDiscoDescription() {
		return "LDAP Server";
	}

	@Override
	public void processPacket(Packet packet) {

	}

	@Override
	public void tlsHandshakeCompleted(LdapIOService<Object> service) {

	}

	@Override
	protected int[] getDefaultPorts() {
		return new int[]{10389};
	}

	@Override
	protected int[] getDefaultSSLPorts() {
		return new int[]{10636};
	}

	@Override
	protected LdapIOService<Object> getIOServiceInstance() throws IOException {
		return new LdapIOService<>();
	}

	@Override
	public void register(Kernel kernel) {
		super.register(kernel);
		kernel.registerBean(TigaseSaslProvider.class).setActive(true).exec();
	}
}
