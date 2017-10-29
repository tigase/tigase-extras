/*
 * TigaseUserMeta.java
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

package tigase.management;

//
// Generated by mibgen version 5.1 (03/08/07) when compiling TIGASE-MANAGEMENT-MIB in standard metadata mode.
//

// java imports
//

import com.sun.management.snmp.*;
import com.sun.management.snmp.agent.*;

import javax.management.MBeanServer;
import java.io.Serializable;

// jmx imports
//
// jdmk imports
//

/**
 * The class is used for representing SNMP metadata for the "TigaseUser" group. The group is defined with the following
 * oid: 1.3.6.1.4.1.16120609.2.145.3.163.1.1.1.
 */
public class TigaseUserMeta
		extends SnmpMibGroup
		implements Serializable, SnmpStandardMetaServer {

	protected TigaseUserMBean node;
	protected SnmpStandardObjectServer objectserver = null;

	/**
	 * Constructor for the metadata associated to "TigaseUser".
	 */
	public TigaseUserMeta(SnmpMib myMib, SnmpStandardObjectServer objserv) {
		objectserver = objserv;
		try {
			registerObject(2);
			registerObject(1);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	/**
	 * Get the value of a scalar variable
	 */
	public SnmpValue get(long var, Object data) throws SnmpStatusException {
		switch ((int) var) {
			case 2:
				return new SnmpCounter64(node.getTigaseUserRegisteredCount());

			case 1:
				return new SnmpGauge(node.getTigaseUserSessionCount());

			default:
				break;
		}
		throw new SnmpStatusException(SnmpStatusException.noSuchObject);
	}

	/**
	 * Set the value of a scalar variable
	 */
	public SnmpValue set(SnmpValue x, long var, Object data) throws SnmpStatusException {
		switch ((int) var) {
			case 2:
				throw new SnmpStatusException(SnmpStatusException.snmpRspNotWritable);

			case 1:
				throw new SnmpStatusException(SnmpStatusException.snmpRspNotWritable);

			default:
				break;
		}
		throw new SnmpStatusException(SnmpStatusException.snmpRspNotWritable);
	}

	// ------------------------------------------------------------
	//
	// Implements the "get" method defined in "SnmpMibGroup".
	// See the "SnmpMibGroup" Javadoc API for more details.
	//
	// ------------------------------------------------------------

	/**
	 * Check the value of a scalar variable
	 */
	public void check(SnmpValue x, long var, Object data) throws SnmpStatusException {
		switch ((int) var) {
			case 2:
				throw new SnmpStatusException(SnmpStatusException.snmpRspNotWritable);

			case 1:
				throw new SnmpStatusException(SnmpStatusException.snmpRspNotWritable);

			default:
				throw new SnmpStatusException(SnmpStatusException.snmpRspNotWritable);
		}
	}

	// ------------------------------------------------------------
	//
	// Implements the "set" method defined in "SnmpMibGroup".
	// See the "SnmpMibGroup" Javadoc API for more details.
	//
	// ------------------------------------------------------------

	public void get(SnmpMibSubRequest req, int depth) throws SnmpStatusException {
		objectserver.get(this, req, depth);
	}

	// ------------------------------------------------------------
	//
	// Implements the "check" method defined in "SnmpMibGroup".
	// See the "SnmpMibGroup" Javadoc API for more details.
	//
	// ------------------------------------------------------------

	public void set(SnmpMibSubRequest req, int depth) throws SnmpStatusException {
		objectserver.set(this, req, depth);
	}

	public void check(SnmpMibSubRequest req, int depth) throws SnmpStatusException {
		objectserver.check(this, req, depth);
	}

	/**
	 * Returns true if "arc" identifies a scalar object.
	 */
	public boolean isVariable(long arc) {

		switch ((int) arc) {
			case 2:
			case 1:
				return true;
			default:
				break;
		}
		return false;
	}

	// ------------------------------------------------------------
	//
	// Implements the "skipVariable" method defined in "SnmpMibGroup".
	// See the "SnmpMibGroup" Javadoc API for more details.
	//
	// ------------------------------------------------------------

	/**
	 * Returns true if "arc" identifies a readable scalar object.
	 */
	public boolean isReadable(long arc) {

		switch ((int) arc) {
			case 2:
			case 1:
				return true;
			default:
				break;
		}
		return false;
	}

	public boolean skipVariable(long var, Object data, int pduVersion) {
		switch ((int) var) {
			case 2:
				if (pduVersion == SnmpDefinitions.snmpVersionOne) {
					return true;
				}
				break;
			default:
				break;
		}
		return false;
	}

	/**
	 * Return the name of the attribute corresponding to the SNMP variable identified by "id".
	 */
	public String getAttributeName(long id) throws SnmpStatusException {
		switch ((int) id) {
			case 2:
				return "TigaseUserRegisteredCount";

			case 1:
				return "TigaseUserSessionCount";

			default:
				break;
		}
		throw new SnmpStatusException(SnmpStatusException.noSuchObject);
	}

	/**
	 * Returns true if "arc" identifies a table object.
	 */
	public boolean isTable(long arc) {

		switch ((int) arc) {
			default:
				break;
		}
		return false;
	}

	/**
	 * Returns the table object identified by "arc".
	 */
	public SnmpMibTable getTable(long arc) {
		return null;
	}

	/**
	 * Register the group's SnmpMibTable objects with the meta-data.
	 */
	public void registerTableNodes(SnmpMib mib, MBeanServer server) {
	}

	/**
	 * Allow to bind the metadata description to a specific object.
	 */
	protected void setInstance(TigaseUserMBean var) {
		node = var;
	}
}
