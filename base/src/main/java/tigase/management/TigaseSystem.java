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
import java.lang.management.ManagementFactory;

import static tigase.management.StatisticsConstatnts.MR_COMP;
import static tigase.management.StatisticsConstatnts.UPTIME_STAT;

/**
 * The class is used for implementing the "TigaseSystem" group. The group is defined with the following oid:
 * 1.3.6.1.4.1.16120609.2.145.3.163.1.1.4.
 */
public class TigaseSystem
		implements TigaseSystemMBean, Serializable {

	/**
	 * Variable for storing the value of "TigaseSystemHeapTotal". The variable is identified by:
	 * "1.3.6.1.4.1.16120609.2.145.3.163.1.1.4.1".
	 */
	protected Long TigaseSystemHeapTotal = new Long(1);
	/**
	 * Variable for storing the value of "TigaseSystemHeapUsed". The variable is identified by:
	 * "1.3.6.1.4.1.16120609.2.145.3.163.1.1.4.2".
	 */
	protected Long TigaseSystemHeapUsed = new Long(1);
	/**
	 * Variable for storing the value of "TigaseSystemNonHeapTotal". The variable is identified by:
	 * "1.3.6.1.4.1.16120609.2.145.3.163.1.1.4.3".
	 */
	protected Long TigaseSystemNonHeapTotal = new Long(1);
	/**
	 * Variable for storing the value of "TigaseSystemNonHeapUsed". The variable is identified by:
	 * "1.3.6.1.4.1.16120609.2.145.3.163.1.1.4.4".
	 */
	protected Long TigaseSystemNonHeapUsed = new Long(1);
	/**
	 * Variable for storing the value of "TigaseSystemUptimeHumanReadable". The variable is identified by:
	 * "1.3.6.1.4.1.16120609.2.145.3.163.1.1.4.6".
	 */
	protected String TigaseSystemUptimeHumanReadable = new String("JDMK 5.1");
	/**
	 * Variable for storing the value of "TigaseSystemUptimeMillis". The variable is identified by:
	 * "1.3.6.1.4.1.16120609.2.145.3.163.1.1.4.5".
	 */
	protected Long TigaseSystemUptimeMillis = new Long(1);

	/**
	 * Constructor for the "TigaseSystem" group. If the group contains a table, the entries created through an SNMP SET
	 * will not be registered in Java DMK.
	 */
	public TigaseSystem(SnmpMib myMib) {
	}

	/**
	 * Constructor for the "TigaseSystem" group. If the group contains a table, the entries created through an SNMP SET
	 * will be AUTOMATICALLY REGISTERED in Java DMK.
	 */
	public TigaseSystem(SnmpMib myMib, MBeanServer server) {
	}

	public String getTigaseSystemUptimeHumanReadable() throws SnmpStatusException {
		TigaseSystemUptimeHumanReadable = StatisticsUtils.getStatsValue(MR_COMP, UPTIME_STAT,
																		TigaseSystemUptimeHumanReadable);
		return TigaseSystemUptimeHumanReadable;
	}

	public Long getTigaseSystemUptimeMillis() throws SnmpStatusException {
		TigaseSystemUptimeMillis = ManagementFactory.getRuntimeMXBean().getUptime();
		return TigaseSystemUptimeMillis;
	}

	public Long getTigaseSystemNonHeapUsed() throws SnmpStatusException {
		TigaseSystemNonHeapUsed = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed();
		return TigaseSystemNonHeapUsed;
	}

	public Long getTigaseSystemNonHeapTotal() throws SnmpStatusException {
		TigaseSystemNonHeapTotal = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getMax();
		return TigaseSystemNonHeapTotal;
	}

	public Long getTigaseSystemHeapUsed() throws SnmpStatusException {
		TigaseSystemHeapUsed = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
		return TigaseSystemHeapUsed;
	}

	public Long getTigaseSystemHeapTotal() throws SnmpStatusException {
		TigaseSystemHeapTotal = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getMax();
		return TigaseSystemHeapTotal;
	}

}
