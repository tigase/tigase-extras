/*
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

import com.sun.management.snmp.SnmpStatusException;
import com.sun.management.snmp.agent.SnmpMib;

import javax.management.MBeanServer;
import java.io.Serializable;

import static tigase.management.StatisticsConstatnts.*;

/**
 * The class is used for implementing the "TigaseConnection" group. The group is defined with the following oid:
 * 1.3.6.1.4.1.16120609.2.145.3.163.1.1.2.
 */
public class TigaseConnection
		implements TigaseConnectionMBean, Serializable {

	/**
	 * Variable for storing the value of "TigaseConnectionBoshCount". The variable is identified by:
	 * "1.3.6.1.4.1.16120609.2.145.3.163.1.1.2.3".
	 */
	protected Long TigaseConnectionBoshCount = new Long(1);
	/**
	 * Variable for storing the value of "TigaseConnectionClientCount". The variable is identified by:
	 * "1.3.6.1.4.1.16120609.2.145.3.163.1.1.2.1".
	 */
	protected Long TigaseConnectionClientCount = new Long(1);
	/**
	 * Variable for storing the value of "TigaseConnectionServerCount". The variable is identified by:
	 * "1.3.6.1.4.1.16120609.2.145.3.163.1.1.2.2".
	 */
	protected Long TigaseConnectionServerCount = new Long(1);

	/**
	 * Constructor for the "TigaseConnection" group. If the group contains a table, the entries created through an SNMP
	 * SET will not be registered in Java DMK.
	 */
	public TigaseConnection(SnmpMib myMib) {
	}

	/**
	 * Constructor for the "TigaseConnection" group. If the group contains a table, the entries created through an SNMP
	 * SET will be AUTOMATICALLY REGISTERED in Java DMK.
	 */
	public TigaseConnection(SnmpMib myMib, MBeanServer server) {
	}

	public Long getTigaseConnectionBoshCount() throws SnmpStatusException {
		TigaseConnectionBoshCount = StatisticsUtils.getStatsValue(BOSH_COMP, OPEN_CONN_STAT, TigaseConnectionBoshCount);
		return TigaseConnectionBoshCount;
	}

	public Long getTigaseConnectionServerCount() throws SnmpStatusException {
		TigaseConnectionServerCount = StatisticsUtils.getStatsValue(SRV_COMP, OPEN_CONN_STAT,
																	TigaseConnectionServerCount);
		return TigaseConnectionServerCount;
	}

	public Long getTigaseConnectionClientCount() throws SnmpStatusException {
		TigaseConnectionClientCount = StatisticsUtils.getStatsValue(CLI_COMP, OPEN_CONN_STAT,
																	TigaseConnectionClientCount);
		return TigaseConnectionClientCount;
	}

}
