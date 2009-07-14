/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 * 
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.management;

import com.sun.management.snmp.agent.SnmpMib;
import com.sun.jdmk.comm.HtmlAdaptorServer;
import com.sun.management.comm.SnmpAdaptorServer;
import com.sun.jndi.rmi.registry.RegistryContextFactory;
import com.sun.management.snmp.IPAcl.JdmkAcl;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.rmi.registry.LocateRegistry;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;
import tigase.conf.MonitoringSetupIfc;

/**
 * Created: Jan 10, 2009 8:45:33 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class MonitoringSetup implements MonitoringSetupIfc {

  private static final Logger log =
		Logger.getLogger(MonitoringSetup.class.getName());

	private SnmpAdaptorServer snmpadaptor = null;
	private static LinkedHashMap<String, Object> mxbeans =
					new LinkedHashMap<String, Object>();

	@Override
	public void initMonitoring(String monitoring, String configDir) {
		if (monitoring == null) {
			return;
		}
		MBeanServer server = ManagementFactory.getPlatformMBeanServer();
		log.config("Installing monitoring services: " + monitoring);
		String[] monitors = monitoring.split(",");
		for (String string : monitors) {
			try {
				String[] mon = string.split(":");
				MONITOR monitor = MONITOR.valueOf(mon[0]);
				int port = Integer.parseInt(mon[1]);
				switch (monitor) {
					case jmx:
						log.config("Loading JMX monitor.");
						LocateRegistry.createRegistry(port);
						String serviceURL = "service:jmx:rmi" +
										":///jndi/rmi" +
										"://localhost:" + mon[1] + "/jmxrmi" +
										"";
						Map<String, String> map = new LinkedHashMap<String, String>();
						map.put("java.naming.factory.initial",
										RegistryContextFactory.class.getName());
						map.put(RMIConnectorServer.JNDI_REBIND_ATTRIBUTE, "true");
						map.put("jmx.remote.x.password.file",
										configDir + File.separator + "jmx.password");
						map.put("jmx.remote.x.access.file",
										configDir + File.separator + "jmx.access");
						JMXConnectorServer connector =
										JMXConnectorServerFactory.newJMXConnectorServer(
										new JMXServiceURL(serviceURL),
										map, server);
						// register the connector server as an MBean
						server.registerMBean(connector,
										new ObjectName("system:name=rmiconnector"));
						//start the connector server
						connector.start();
						break;
					case http:
						log.config("Loading HTTP monitor.");
						HtmlAdaptorServer adaptor = new HtmlAdaptorServer();
						ObjectName httpName = new ObjectName("localhost" +
										":class=HtmlAdaptorServer,protocol=html,port=" +port);
						server.registerMBean(adaptor, httpName);
						adaptor.setPort(port);
						adaptor.start();
						break;
					case snmp:
						log.config("Loading SNMP monitor.");
						ObjectName snmpName = new ObjectName("localhost" +
										":class=SnmpAdaptorServer,protocol=snmp,port=" + port);
						JdmkAcl acl = new JdmkAcl("tigase",
										configDir + File.separator + "snmp.acl");
						snmpadaptor = new SnmpAdaptorServer(acl, port);
						server.registerMBean(snmpadaptor, snmpName);
						snmpadaptor.start();
						break;
				}
			} catch (Exception e) {
				log.log(Level.WARNING, "Can not start the monitor: " + string + ": ",
								e);
			}
		}
	}

	@Override
	public void initializationCompleted() {
		log.config("initializationCompleted.");
		if (snmpadaptor != null) {
			MBeanServer server = ManagementFactory.getPlatformMBeanServer();
			try {
				String className = "tigase.management.TIGASE_MANAGEMENT_MIB";
				String objName = "snmp:class=TIGASE_MANAGEMENT_MIB";
				log.config("Initializing Tiase MIB MXBean");
				ObjectName mibObjName = new ObjectName(objName);
				SnmpMib mib = (SnmpMib) Class.forName(className).newInstance();
				server.registerMBean(mib, mibObjName);
				snmpadaptor.addMib(mib);
				putMXBean(objName, mib);
			} catch (Exception e) {
				log.log(Level.WARNING, "Can not load Tigase monitoring MXBean: ", e);
			}
		} else {
			log.config("snmpadaptor not installed, skipping MIB loading.");
		}

	}

	@Override
	public void putMXBean(String objName, Object bean) {
		mxbeans.put(objName, bean);
	}

	@Override
	public Object getMXBean(String objName) {
		return mxbeans.get(objName);
	}


}
