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

//
// Generated by mibgen version 5.1 (03/08/07) when compiling TIGASE-MANAGEMENT-MIB in standard metadata mode.
//

// java imports
//

import com.sun.management.snmp.agent.SnmpMib;
import com.sun.management.snmp.agent.SnmpMibTable;
import com.sun.management.snmp.agent.SnmpStandardObjectServer;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.Serializable;
import java.util.Hashtable;

// jmx imports
//
// jdmk imports
//

/**
 * The class is used for representing "TIGASE-MANAGEMENT-MIB". You can edit the file if you want to modify the behavior
 * of the MIB.
 */
public class TIGASE_MANAGEMENT_MIB
		extends SnmpMib
		implements Serializable {

	protected final Hashtable metadatas = new Hashtable();
	protected SnmpStandardObjectServer objectserver;
	private boolean isInitialized = false;

	/**
	 * Default constructor. Initialize the Mib tree.
	 */
	public TIGASE_MANAGEMENT_MIB() {
		mibName = "TIGASE_MANAGEMENT_MIB";
	}

	// ------------------------------------------------------------
	//
	// Initialization of the "TigaseSystem" group.
	//
	// ------------------------------------------------------------

	/**
	 * Initialization of the MIB with no registration in Java DMK.
	 */
	public void init() throws IllegalAccessException {
		// Allow only one initialization of the MIB.
		//
		if (isInitialized == true) {
			return;
		}

		try {
			populate(null, null);
		} catch (IllegalAccessException x) {
			throw x;
		} catch (RuntimeException x) {
			throw x;
		} catch (Exception x) {
			throw new Error(x.getMessage());
		}

		isInitialized = true;
	}

	/**
	 * Initialization of the MIB with AUTOMATIC REGISTRATION in Java DMK.
	 */
	public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
		// Allow only one initialization of the MIB.
		//
		if (isInitialized == true) {
			throw new InstanceAlreadyExistsException();
		}

		// Initialize MBeanServer information.
		//
		this.server = server;

		populate(server, name);

		isInitialized = true;
		return name;
	}

	/**
	 * Initialization of the MIB with no registration in Java DMK.
	 */
	public void populate(MBeanServer server, ObjectName name) throws Exception {
		// Allow only one initialization of the MIB.
		//
		if (isInitialized == true) {
			return;
		}

		if (objectserver == null) {
			objectserver = new SnmpStandardObjectServer();
		}

		// Initialization of the "TigaseSystem" group.
		// To disable support of this group, redefine the
		// "createTigaseSystemMetaNode()" factory method, and make it return "null"
		//
		initTigaseSystem(server);

		// Initialization of the "TigaseLoad" group.
		// To disable support of this group, redefine the
		// "createTigaseLoadMetaNode()" factory method, and make it return "null"
		//
		initTigaseLoad(server);

		// Initialization of the "TigaseConnection" group.
		// To disable support of this group, redefine the
		// "createTigaseConnectionMetaNode()" factory method, and make it return "null"
		//
		initTigaseConnection(server);

		// Initialization of the "TigaseUser" group.
		// To disable support of this group, redefine the
		// "createTigaseUserMetaNode()" factory method, and make it return "null"
		//
		initTigaseUser(server);

		isInitialized = true;
	}

	// ------------------------------------------------------------
	//
	// Initialization of the "TigaseLoad" group.
	//
	// ------------------------------------------------------------

	public void registerTableMeta(String name, SnmpMibTable meta) {
		if (metadatas == null) {
			return;
		}
		if (name == null) {
			return;
		}
		metadatas.put(name, meta);
	}

	public SnmpMibTable getRegisteredTableMeta(String name) {
		if (metadatas == null) {
			return null;
		}
		if (name == null) {
			return null;
		}
		return (SnmpMibTable) metadatas.get(name);
	}

	public SnmpStandardObjectServer getStandardObjectServer() {
		if (objectserver == null) {
			objectserver = new SnmpStandardObjectServer();
		}
		return objectserver;
	}

	// ------------------------------------------------------------
	//
	// Initialization of the "TigaseConnection" group.
	//
	// ------------------------------------------------------------

	/**
	 * Initialization of the "TigaseSystem" group.
	 * <p>
	 * To disable support of this group, redefine the "createTigaseSystemMetaNode()" factory method, and make it return
	 * "null"
	 *
	 * @param server MBeanServer for this group (may be null)
	 **/
	protected void initTigaseSystem(MBeanServer server) throws Exception {
		final String oid = getGroupOid("TigaseSystem", "1.3.6.1.4.1.16120609.2.145.3.163.1.1.4");
		ObjectName objname = null;
		if (server != null) {
			objname = getGroupObjectName("TigaseSystem", oid, mibName + ":name=tigase.management.TigaseSystem");
		}
		final TigaseSystemMeta meta = createTigaseSystemMetaNode("TigaseSystem", oid, objname, server);
		if (meta != null) {
			meta.registerTableNodes(this, server);

			// Note that when using standard metadata,
			// the returned object must implement the "TigaseSystemMBean"
			// interface.
			//
			final TigaseSystemMBean group = (TigaseSystemMBean) createTigaseSystemMBean("TigaseSystem", oid, objname,
																						server);
			meta.setInstance(group);
			registerGroupNode("TigaseSystem", oid, objname, meta, group, server);
		}
	}

	/**
	 * Factory method for "TigaseSystem" group metadata class.
	 * <p>
	 * You can redefine this method if you need to replace the default generated metadata class with your own customized
	 * class.
	 *
	 * @param groupName Name of the group ("TigaseSystem")
	 * @param groupOid OID of this group
	 * @param groupObjname ObjectName for this group (may be null)
	 * @param server MBeanServer for this group (may be null)
	 *
	 * @return An instance of the metadata class generated for the "TigaseSystem" group (TigaseSystemMeta)
	 **/
	protected TigaseSystemMeta createTigaseSystemMetaNode(String groupName, String groupOid, ObjectName groupObjname,
														  MBeanServer server) {
		return new TigaseSystemMeta(this, objectserver);
	}

	/**
	 * Factory method for "TigaseSystem" group MBean.
	 * <p>
	 * You can redefine this method if you need to replace the default generated MBean class with your own customized
	 * class.
	 *
	 * @param groupName Name of the group ("TigaseSystem")
	 * @param groupOid OID of this group
	 * @param groupObjname ObjectName for this group (may be null)
	 * @param server MBeanServer for this group (may be null)
	 *
	 * @return An instance of the MBean class generated for the "TigaseSystem" group (TigaseSystem)
	 * <p>
	 * Note that when using standard metadata, the returned object must implement the "TigaseSystemMBean" interface.
	 **/
	protected Object createTigaseSystemMBean(String groupName, String groupOid, ObjectName groupObjname,
											 MBeanServer server) {

		// Note that when using standard metadata,
		// the returned object must implement the "TigaseSystemMBean"
		// interface.
		//
		if (server != null) {
			return new TigaseSystem(this, server);
		} else {
			return new TigaseSystem(this);
		}
	}

	// ------------------------------------------------------------
	//
	// Initialization of the "TigaseUser" group.
	//
	// ------------------------------------------------------------

	/**
	 * Initialization of the "TigaseLoad" group.
	 * <p>
	 * To disable support of this group, redefine the "createTigaseLoadMetaNode()" factory method, and make it return
	 * "null"
	 *
	 * @param server MBeanServer for this group (may be null)
	 **/
	protected void initTigaseLoad(MBeanServer server) throws Exception {
		final String oid = getGroupOid("TigaseLoad", "1.3.6.1.4.1.16120609.2.145.3.163.1.1.3");
		ObjectName objname = null;
		if (server != null) {
			objname = getGroupObjectName("TigaseLoad", oid, mibName + ":name=tigase.management.TigaseLoad");
		}
		final TigaseLoadMeta meta = createTigaseLoadMetaNode("TigaseLoad", oid, objname, server);
		if (meta != null) {
			meta.registerTableNodes(this, server);

			// Note that when using standard metadata,
			// the returned object must implement the "TigaseLoadMBean"
			// interface.
			//
			final TigaseLoadMBean group = (TigaseLoadMBean) createTigaseLoadMBean("TigaseLoad", oid, objname, server);
			meta.setInstance(group);
			registerGroupNode("TigaseLoad", oid, objname, meta, group, server);
		}
	}

	/**
	 * Factory method for "TigaseLoad" group metadata class.
	 * <p>
	 * You can redefine this method if you need to replace the default generated metadata class with your own customized
	 * class.
	 *
	 * @param groupName Name of the group ("TigaseLoad")
	 * @param groupOid OID of this group
	 * @param groupObjname ObjectName for this group (may be null)
	 * @param server MBeanServer for this group (may be null)
	 *
	 * @return An instance of the metadata class generated for the "TigaseLoad" group (TigaseLoadMeta)
	 **/
	protected TigaseLoadMeta createTigaseLoadMetaNode(String groupName, String groupOid, ObjectName groupObjname,
													  MBeanServer server) {
		return new TigaseLoadMeta(this, objectserver);
	}

	/**
	 * Factory method for "TigaseLoad" group MBean.
	 * <p>
	 * You can redefine this method if you need to replace the default generated MBean class with your own customized
	 * class.
	 *
	 * @param groupName Name of the group ("TigaseLoad")
	 * @param groupOid OID of this group
	 * @param groupObjname ObjectName for this group (may be null)
	 * @param server MBeanServer for this group (may be null)
	 *
	 * @return An instance of the MBean class generated for the "TigaseLoad" group (TigaseLoad)
	 * <p>
	 * Note that when using standard metadata, the returned object must implement the "TigaseLoadMBean" interface.
	 **/
	protected Object createTigaseLoadMBean(String groupName, String groupOid, ObjectName groupObjname,
										   MBeanServer server) {

		// Note that when using standard metadata,
		// the returned object must implement the "TigaseLoadMBean"
		// interface.
		//
		if (server != null) {
			return new TigaseLoad(this, server);
		} else {
			return new TigaseLoad(this);
		}
	}

	// ------------------------------------------------------------
	//
	// Implements the "registerTableMeta" method defined in "SnmpMib".
	// See the "SnmpMib" Javadoc API for more details.
	//
	// ------------------------------------------------------------

	/**
	 * Initialization of the "TigaseConnection" group.
	 * <p>
	 * To disable support of this group, redefine the "createTigaseConnectionMetaNode()" factory method, and make it
	 * return "null"
	 *
	 * @param server MBeanServer for this group (may be null)
	 **/
	protected void initTigaseConnection(MBeanServer server) throws Exception {
		final String oid = getGroupOid("TigaseConnection", "1.3.6.1.4.1.16120609.2.145.3.163.1.1.2");
		ObjectName objname = null;
		if (server != null) {
			objname = getGroupObjectName("TigaseConnection", oid, mibName + ":name=tigase.management.TigaseConnection");
		}
		final TigaseConnectionMeta meta = createTigaseConnectionMetaNode("TigaseConnection", oid, objname, server);
		if (meta != null) {
			meta.registerTableNodes(this, server);

			// Note that when using standard metadata,
			// the returned object must implement the "TigaseConnectionMBean"
			// interface.
			//
			final TigaseConnectionMBean group = (TigaseConnectionMBean) createTigaseConnectionMBean("TigaseConnection",
																									oid, objname,
																									server);
			meta.setInstance(group);
			registerGroupNode("TigaseConnection", oid, objname, meta, group, server);
		}
	}

	// ------------------------------------------------------------
	//
	// Implements the "getRegisteredTableMeta" method defined in "SnmpMib".
	// See the "SnmpMib" Javadoc API for more details.
	//
	// ------------------------------------------------------------

	/**
	 * Factory method for "TigaseConnection" group metadata class.
	 * <p>
	 * You can redefine this method if you need to replace the default generated metadata class with your own customized
	 * class.
	 *
	 * @param groupName Name of the group ("TigaseConnection")
	 * @param groupOid OID of this group
	 * @param groupObjname ObjectName for this group (may be null)
	 * @param server MBeanServer for this group (may be null)
	 *
	 * @return An instance of the metadata class generated for the "TigaseConnection" group (TigaseConnectionMeta)
	 **/
	protected TigaseConnectionMeta createTigaseConnectionMetaNode(String groupName, String groupOid,
																  ObjectName groupObjname, MBeanServer server) {
		return new TigaseConnectionMeta(this, objectserver);
	}

	/**
	 * Factory method for "TigaseConnection" group MBean.
	 * <p>
	 * You can redefine this method if you need to replace the default generated MBean class with your own customized
	 * class.
	 *
	 * @param groupName Name of the group ("TigaseConnection")
	 * @param groupOid OID of this group
	 * @param groupObjname ObjectName for this group (may be null)
	 * @param server MBeanServer for this group (may be null)
	 *
	 * @return An instance of the MBean class generated for the "TigaseConnection" group (TigaseConnection)
	 * <p>
	 * Note that when using standard metadata, the returned object must implement the "TigaseConnectionMBean"
	 * interface.
	 **/
	protected Object createTigaseConnectionMBean(String groupName, String groupOid, ObjectName groupObjname,
												 MBeanServer server) {

		// Note that when using standard metadata,
		// the returned object must implement the "TigaseConnectionMBean"
		// interface.
		//
		if (server != null) {
			return new TigaseConnection(this, server);
		} else {
			return new TigaseConnection(this);
		}
	}

	/**
	 * Initialization of the "TigaseUser" group.
	 * <p>
	 * To disable support of this group, redefine the "createTigaseUserMetaNode()" factory method, and make it return
	 * "null"
	 *
	 * @param server MBeanServer for this group (may be null)
	 **/
	protected void initTigaseUser(MBeanServer server) throws Exception {
		final String oid = getGroupOid("TigaseUser", "1.3.6.1.4.1.16120609.2.145.3.163.1.1.1");
		ObjectName objname = null;
		if (server != null) {
			objname = getGroupObjectName("TigaseUser", oid, mibName + ":name=tigase.management.TigaseUser");
		}
		final TigaseUserMeta meta = createTigaseUserMetaNode("TigaseUser", oid, objname, server);
		if (meta != null) {
			meta.registerTableNodes(this, server);

			// Note that when using standard metadata,
			// the returned object must implement the "TigaseUserMBean"
			// interface.
			//
			final TigaseUserMBean group = (TigaseUserMBean) createTigaseUserMBean("TigaseUser", oid, objname, server);
			meta.setInstance(group);
			registerGroupNode("TigaseUser", oid, objname, meta, group, server);
		}
	}

	/**
	 * Factory method for "TigaseUser" group metadata class.
	 * <p>
	 * You can redefine this method if you need to replace the default generated metadata class with your own customized
	 * class.
	 *
	 * @param groupName Name of the group ("TigaseUser")
	 * @param groupOid OID of this group
	 * @param groupObjname ObjectName for this group (may be null)
	 * @param server MBeanServer for this group (may be null)
	 *
	 * @return An instance of the metadata class generated for the "TigaseUser" group (TigaseUserMeta)
	 **/
	protected TigaseUserMeta createTigaseUserMetaNode(String groupName, String groupOid, ObjectName groupObjname,
													  MBeanServer server) {
		return new TigaseUserMeta(this, objectserver);
	}

	/**
	 * Factory method for "TigaseUser" group MBean.
	 * <p>
	 * You can redefine this method if you need to replace the default generated MBean class with your own customized
	 * class.
	 *
	 * @param groupName Name of the group ("TigaseUser")
	 * @param groupOid OID of this group
	 * @param groupObjname ObjectName for this group (may be null)
	 * @param server MBeanServer for this group (may be null)
	 *
	 * @return An instance of the MBean class generated for the "TigaseUser" group (TigaseUser)
	 * <p>
	 * Note that when using standard metadata, the returned object must implement the "TigaseUserMBean" interface.
	 **/
	protected Object createTigaseUserMBean(String groupName, String groupOid, ObjectName groupObjname,
										   MBeanServer server) {

		// Note that when using standard metadata,
		// the returned object must implement the "TigaseUserMBean"
		// interface.
		//
		if (server != null) {
			return new TigaseUser(this, server);
		} else {
			return new TigaseUser(this);
		}
	}
}
