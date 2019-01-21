/**
 * Tigase Server Extras Base - Extra modules to Tigase Server
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
package tigase.management;

import com.sun.jdmk.comm.HtmlAdaptorServer;
import com.sun.management.comm.SnmpAdaptorServer;
import com.sun.management.snmp.IPAcl.JdmkAcl;
import com.sun.management.snmp.agent.SnmpMib;
import tigase.conf.MonitoringBeanIfc;
import tigase.kernel.beans.*;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.beans.selector.ConfigType;
import tigase.kernel.beans.selector.ConfigTypeEnum;
import tigase.kernel.core.Kernel;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.rmi.registry.LocateRegistry;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by andrzej on 13.10.2016.
 */
@Bean(name = "monitoring", parent = Kernel.class, active = true, exportable = true)
@ConfigType({ConfigTypeEnum.DefaultMode, ConfigTypeEnum.SessionManagerMode, ConfigTypeEnum.ConnectionManagersMode,
			 ConfigTypeEnum.ComponentMode})
public class MonitoringBean
		implements MonitoringBeanIfc, RegistrarBean {

	@Inject(nullAllowed = true)
	private MonitorBean[] monitors;

	@Override
	public void register(Kernel kernel) {

	}

	@Override
	public void unregister(Kernel kernel) {

	}

	public interface MonitorBean
			extends UnregisterAware, Initializable {

		void stop() throws Exception;

		void start() throws Exception;

	}

	@Bean(name = "http", parent = MonitoringBean.class, active = false)
	public static class HttpMonitor
			extends MonitorBeanAbstract {

		private static final Logger log = Logger.getLogger(HttpMonitor.class.getCanonicalName());

		private HtmlAdaptorServer adaptor;
		private ObjectName httpName;

		public HttpMonitor() {

		}

		@Override
		public void start() throws Exception {
			log.config("Loading HTTP monitor.");

			adaptor = new HtmlAdaptorServer();
			httpName = new ObjectName("localhost" + ":class=HtmlAdaptorServer,protocol=html,port=" + port);

			server.registerMBean(adaptor, httpName);
			adaptor.setPort(port);
			adaptor.start();
		}

		@Override
		public void stop() throws Exception {
			if (adaptor != null) {
				adaptor.stop();
			}
			if (httpName != null) {
				server.unregisterMBean(httpName);
			}
		}
	}

	@Bean(name = "jmx", parent = MonitoringBean.class, active = false)
	public static class JMXMonitor
			extends MonitorBeanAbstract {

		private static final Logger log = Logger.getLogger(JMXMonitor.class.getCanonicalName());
		private JMXConnectorServer connector;
		private ObjectName objectName;

		public JMXMonitor() {

		}

		@Override
		public void start() throws Exception {
			log.config("Loading JMX monitor.");

			objectName = new ObjectName("system:name=rmiconnector");
			LocateRegistry.createRegistry(port);

			String serviceURL = "service:jmx:rmi://localhost:" + port + "/jndi/rmi://localhost:" + port + "/jmxrmi";

			Map<String, String> map = new LinkedHashMap<String, String>();

			map.put("java.naming.factory.initial", "com.sun.jndi.rmi.registry.RegistryContextFactory");
			map.put(RMIConnectorServer.JNDI_REBIND_ATTRIBUTE, "true");
			map.put("jmx.remote.x.password.file", configDir + File.separator + "jmx.password");
			map.put("jmx.remote.x.access.file", configDir + File.separator + "jmx.access");

			connector = JMXConnectorServerFactory.newJMXConnectorServer(new JMXServiceURL(serviceURL), map, server);

			// register the connector server as an MBean
			server.registerMBean(connector, objectName);

			// start the connector server
			connector.start();
		}

		@Override
		public void stop() throws Exception {
			if (connector != null) {
				connector.stop();
			}
			if (objectName != null) {
				server.unregisterMBean(objectName);
			}
		}

	}

	public static abstract class MonitorBeanAbstract
			implements MonitorBean {

		private static final Logger log = Logger.getLogger(MonitorBeanAbstract.class.getCanonicalName());
		@ConfigField(desc = "Config directory", alias = "configDir")
		protected String configDir = "etc";
		@ConfigField(desc = "Port for monitor")
		protected int port;
		protected MBeanServer server;

		public MonitorBeanAbstract() {
			server = ManagementFactory.getPlatformMBeanServer();
		}

		@Override
		public void beforeUnregister() {
			try {
				stop();
			} catch (Exception ex) {
				log.log(Level.SEVERE, "Cound not stop monitor: " + this.getClass().getCanonicalName(), ex);
			}
		}

		@Override
		public void initialize() {
			try {
				start();
			} catch (Exception ex) {
				log.log(Level.SEVERE, "Cound not start monitor: " + this.getClass().getCanonicalName(), ex);
				beforeUnregister();
			}
		}
	}

	@Bean(name = "snmp", parent = MonitoringBean.class, active = false)
	public static class SnmpMonitor
			extends MonitorBeanAbstract {

		private static final Logger log = Logger.getLogger(SnmpMonitor.class.getCanonicalName());
		private SnmpMib mib = null;
		private ObjectName mibObjName;
		private ObjectName snmpName;
		private SnmpAdaptorServer snmpadaptor = null;

		public SnmpMonitor() {

		}

		@Override
		public void start() throws Exception {
			log.config("Loading SNMP monitor.");

			snmpName = new ObjectName("localhost" + ":class=SnmpAdaptorServer,protocol=snmp,port=" + port);
			JdmkAcl acl = new JdmkAcl("tigase", configDir + File.separator + "snmp.acl");

			snmpadaptor = new SnmpAdaptorServer(acl, port);
			server.registerMBean(snmpadaptor, snmpName);
			snmpadaptor.start();

			String className = "tigase.management.TIGASE_MANAGEMENT_MIB";
			String objName = "snmp:class=TIGASE_MANAGEMENT_MIB";

			log.config("Initializing Tiase MIB MXBean");

			mibObjName = new ObjectName(objName);
			mib = (SnmpMib) Class.forName(className).newInstance();

			server.registerMBean(mib, mibObjName);
			snmpadaptor.addMib(mib);
		}

		@Override
		public void stop() throws Exception {
			if (snmpadaptor != null) {
				snmpadaptor.stop();
			}
			if (mibObjName != null) {
				server.unregisterMBean(mibObjName);
			}
			if (snmpName != null) {
				server.unregisterMBean(snmpName);
			}
		}
	}
}
