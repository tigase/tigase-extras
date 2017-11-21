/*
 * TigaseLoad.java
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

import com.sun.management.snmp.SnmpStatusException;
import com.sun.management.snmp.agent.SnmpMib;

import javax.management.MBeanServer;
import java.io.Serializable;

import static tigase.management.StatisticsConstatnts.*;

/**
 * The class is used for implementing the "TigaseLoad" group. The group is defined with the following oid:
 * 1.3.6.1.4.1.16120609.2.145.3.163.1.1.3.
 */
public class TigaseLoad
		implements TigaseLoadMBean, Serializable {

	/**
	 * Variable for storing the value of "TigaseLoadBoshLastHour". The variable is identified by:
	 * "1.3.6.1.4.1.16120609.2.145.3.163.1.1.3.21".
	 */
	protected Long TigaseLoadBoshLastHour = new Long(1);
	/**
	 * Variable for storing the value of "TigaseLoadBoshLastMinute". The variable is identified by:
	 * "1.3.6.1.4.1.16120609.2.145.3.163.1.1.3.20".
	 */
	protected Long TigaseLoadBoshLastMinute = new Long(1);
	/**
	 * Variable for storing the value of "TigaseLoadBoshLastSecond". The variable is identified by:
	 * "1.3.6.1.4.1.16120609.2.145.3.163.1.1.3.19".
	 */
	protected Long TigaseLoadBoshLastSecond = new Long(1);
	/**
	 * Variable for storing the value of "TigaseLoadC2SLastHour". The variable is identified by:
	 * "1.3.6.1.4.1.16120609.2.145.3.163.1.1.3.18".
	 */
	protected Long TigaseLoadC2SLastHour = new Long(1);
	/**
	 * Variable for storing the value of "TigaseLoadC2SLastMinute". The variable is identified by:
	 * "1.3.6.1.4.1.16120609.2.145.3.163.1.1.3.17".
	 */
	protected Long TigaseLoadC2SLastMinute = new Long(1);
	/**
	 * Variable for storing the value of "TigaseLoadC2SLastSecond". The variable is identified by:
	 * "1.3.6.1.4.1.16120609.2.145.3.163.1.1.3.16".
	 */
	protected Long TigaseLoadC2SLastSecond = new Long(1);
	/**
	 * Variable for storing the value of "TigaseLoadMRLastHour". The variable is identified by:
	 * "1.3.6.1.4.1.16120609.2.145.3.163.1.1.3.6".
	 */
	protected Long TigaseLoadMRLastHour = new Long(1);
	/**
	 * Variable for storing the value of "TigaseLoadMRLastMinute". The variable is identified by:
	 * "1.3.6.1.4.1.16120609.2.145.3.163.1.1.3.5".
	 */
	protected Long TigaseLoadMRLastMinute = new Long(1);
	/**
	 * Variable for storing the value of "TigaseLoadMRLastSecond". The variable is identified by:
	 * "1.3.6.1.4.1.16120609.2.145.3.163.1.1.3.4".
	 */
	protected Long TigaseLoadMRLastSecond = new Long(1);
	/**
	 * Variable for storing the value of "TigaseLoadMUCLastHour". The variable is identified by:
	 * "1.3.6.1.4.1.16120609.2.145.3.163.1.1.3.9".
	 */
	protected Long TigaseLoadMUCLastHour = new Long(1);
	/**
	 * Variable for storing the value of "TigaseLoadMUCLastMinute". The variable is identified by:
	 * "1.3.6.1.4.1.16120609.2.145.3.163.1.1.3.8".
	 */
	protected Long TigaseLoadMUCLastMinute = new Long(1);
	/**
	 * Variable for storing the value of "TigaseLoadMUCLastSecond". The variable is identified by:
	 * "1.3.6.1.4.1.16120609.2.145.3.163.1.1.3.7".
	 */
	protected Long TigaseLoadMUCLastSecond = new Long(1);
	/**
	 * Variable for storing the value of "TigaseLoadPubSubLastHour". The variable is identified by:
	 * "1.3.6.1.4.1.16120609.2.145.3.163.1.1.3.12".
	 */
	protected Long TigaseLoadPubSubLastHour = new Long(1);
	/**
	 * Variable for storing the value of "TigaseLoadPubSubLastMinute". The variable is identified by:
	 * "1.3.6.1.4.1.16120609.2.145.3.163.1.1.3.11".
	 */
	protected Long TigaseLoadPubSubLastMinute = new Long(1);
	/**
	 * Variable for storing the value of "TigaseLoadPubSubLastSecond". The variable is identified by:
	 * "1.3.6.1.4.1.16120609.2.145.3.163.1.1.3.10".
	 */
	protected Long TigaseLoadPubSubLastSecond = new Long(1);
	/**
	 * Variable for storing the value of "TigaseLoadS2SLastHour". The variable is identified by:
	 * "1.3.6.1.4.1.16120609.2.145.3.163.1.1.3.15".
	 */
	protected Long TigaseLoadS2SLastHour = new Long(1);
	/**
	 * Variable for storing the value of "TigaseLoadS2SLastMinute". The variable is identified by:
	 * "1.3.6.1.4.1.16120609.2.145.3.163.1.1.3.14".
	 */
	protected Long TigaseLoadS2SLastMinute = new Long(1);
	/**
	 * Variable for storing the value of "TigaseLoadS2SLastSecond". The variable is identified by:
	 * "1.3.6.1.4.1.16120609.2.145.3.163.1.1.3.13".
	 */
	protected Long TigaseLoadS2SLastSecond = new Long(1);
	/**
	 * Variable for storing the value of "TigaseLoadSMLastHour". The variable is identified by:
	 * "1.3.6.1.4.1.16120609.2.145.3.163.1.1.3.3".
	 */
	protected Long TigaseLoadSMLastHour = new Long(1);
	/**
	 * Variable for storing the value of "TigaseLoadSMLastMinute". The variable is identified by:
	 * "1.3.6.1.4.1.16120609.2.145.3.163.1.1.3.2".
	 */
	protected Long TigaseLoadSMLastMinute = new Long(1);
	/**
	 * Variable for storing the value of "TigaseLoadSMLastSecond". The variable is identified by:
	 * "1.3.6.1.4.1.16120609.2.145.3.163.1.1.3.1".
	 */
	protected Long TigaseLoadSMLastSecond = new Long(1);

	/**
	 * Constructor for the "TigaseLoad" group. If the group contains a table, the entries created through an SNMP SET
	 * will not be registered in Java DMK.
	 */
	public TigaseLoad(SnmpMib myMib) {
	}

	/**
	 * Constructor for the "TigaseLoad" group. If the group contains a table, the entries created through an SNMP SET
	 * will be AUTOMATICALLY REGISTERED in Java DMK.
	 */
	public TigaseLoad(SnmpMib myMib, MBeanServer server) {
	}

	public Long getTigaseLoadBoshLastSecond() throws SnmpStatusException {
		TigaseLoadBoshLastSecond = StatisticsUtils.getStatsValue(BOSH_COMP, LAST_SECOND_STAT, TigaseLoadBoshLastSecond);
		return TigaseLoadBoshLastSecond;
	}

	public Long getTigaseLoadC2SLastHour() throws SnmpStatusException {
		TigaseLoadC2SLastHour = StatisticsUtils.getStatsValue(CLI_COMP, LAST_HOUR_STAT, TigaseLoadC2SLastHour);
		return TigaseLoadC2SLastHour;
	}

	public Long getTigaseLoadC2SLastMinute() throws SnmpStatusException {
		TigaseLoadC2SLastMinute = StatisticsUtils.getStatsValue(CLI_COMP, LAST_MINUTE_STAT, TigaseLoadC2SLastMinute);
		return TigaseLoadC2SLastMinute;
	}

	public Long getTigaseLoadC2SLastSecond() throws SnmpStatusException {
		TigaseLoadC2SLastSecond = StatisticsUtils.getStatsValue(CLI_COMP, LAST_SECOND_STAT, TigaseLoadC2SLastSecond);
		return TigaseLoadC2SLastSecond;
	}

	public Long getTigaseLoadS2SLastHour() throws SnmpStatusException {
		TigaseLoadS2SLastHour = StatisticsUtils.getStatsValue(SRV_COMP, LAST_HOUR_STAT, TigaseLoadS2SLastHour);
		return TigaseLoadS2SLastHour;
	}

	public Long getTigaseLoadS2SLastMinute() throws SnmpStatusException {
		TigaseLoadS2SLastMinute = StatisticsUtils.getStatsValue(SRV_COMP, LAST_MINUTE_STAT, TigaseLoadS2SLastMinute);
		return TigaseLoadS2SLastMinute;
	}

	public Long getTigaseLoadS2SLastSecond() throws SnmpStatusException {
		TigaseLoadS2SLastSecond = StatisticsUtils.getStatsValue(SRV_COMP, LAST_SECOND_STAT, TigaseLoadS2SLastSecond);
		return TigaseLoadS2SLastSecond;
	}

	public Long getTigaseLoadPubSubLastHour() throws SnmpStatusException {
		TigaseLoadPubSubLastHour = StatisticsUtils.getStatsValue(PUBSUB_COMP, LAST_HOUR_STAT, TigaseLoadPubSubLastHour);
		return TigaseLoadPubSubLastHour;
	}

	public Long getTigaseLoadPubSubLastMinute() throws SnmpStatusException {
		TigaseLoadPubSubLastMinute = StatisticsUtils.getStatsValue(PUBSUB_COMP, LAST_MINUTE_STAT,
																   TigaseLoadPubSubLastMinute);
		return TigaseLoadPubSubLastMinute;
	}

	public Long getTigaseLoadPubSubLastSecond() throws SnmpStatusException {
		TigaseLoadPubSubLastSecond = StatisticsUtils.getStatsValue(PUBSUB_COMP, LAST_SECOND_STAT,
																   TigaseLoadPubSubLastSecond);
		return TigaseLoadPubSubLastSecond;
	}

	public Long getTigaseLoadMUCLastHour() throws SnmpStatusException {
		TigaseLoadMUCLastHour = StatisticsUtils.getStatsValue(MUC_COMP, LAST_HOUR_STAT, TigaseLoadMUCLastHour);
		return TigaseLoadMUCLastHour;
	}

	public Long getTigaseLoadMUCLastMinute() throws SnmpStatusException {
		TigaseLoadMUCLastMinute = StatisticsUtils.getStatsValue(MUC_COMP, LAST_MINUTE_STAT, TigaseLoadMUCLastMinute);
		return TigaseLoadMUCLastMinute;
	}

	public Long getTigaseLoadMUCLastSecond() throws SnmpStatusException {
		TigaseLoadMUCLastSecond = StatisticsUtils.getStatsValue(MUC_COMP, LAST_SECOND_STAT, TigaseLoadMUCLastSecond);
		return TigaseLoadMUCLastSecond;
	}

	public Long getTigaseLoadMRLastHour() throws SnmpStatusException {
		TigaseLoadMRLastHour = StatisticsUtils.getStatsValue(MR_COMP, LAST_HOUR_STAT, TigaseLoadMRLastHour);
		return TigaseLoadMRLastHour;
	}

	public Long getTigaseLoadMRLastMinute() throws SnmpStatusException {
		TigaseLoadMRLastMinute = StatisticsUtils.getStatsValue(MR_COMP, LAST_MINUTE_STAT, TigaseLoadMRLastMinute);
		return TigaseLoadMRLastMinute;
	}

	public Long getTigaseLoadMRLastSecond() throws SnmpStatusException {
		TigaseLoadMRLastSecond = StatisticsUtils.getStatsValue(MR_COMP, LAST_SECOND_STAT, TigaseLoadMRLastSecond);
		return TigaseLoadMRLastSecond;
	}

	public Long getTigaseLoadSMLastHour() throws SnmpStatusException {
		TigaseLoadSMLastHour = StatisticsUtils.getStatsValue(SM_COMP, LAST_HOUR_STAT, TigaseLoadSMLastHour);
		return TigaseLoadSMLastHour;
	}

	public Long getTigaseLoadSMLastMinute() throws SnmpStatusException {
		TigaseLoadSMLastMinute = StatisticsUtils.getStatsValue(SM_COMP, LAST_MINUTE_STAT, TigaseLoadSMLastMinute);
		return TigaseLoadSMLastMinute;
	}

	public Long getTigaseLoadSMLastSecond() throws SnmpStatusException {
		TigaseLoadSMLastSecond = StatisticsUtils.getStatsValue(SM_COMP, LAST_SECOND_STAT, TigaseLoadSMLastSecond);
		return TigaseLoadSMLastSecond;
	}

	public Long getTigaseLoadBoshLastHour() throws SnmpStatusException {
		TigaseLoadBoshLastHour = StatisticsUtils.getStatsValue(BOSH_COMP, LAST_HOUR_STAT, TigaseLoadBoshLastHour);
		return TigaseLoadBoshLastHour;
	}

	public Long getTigaseLoadBoshLastMinute() throws SnmpStatusException {
		TigaseLoadBoshLastMinute = StatisticsUtils.getStatsValue(BOSH_COMP, LAST_MINUTE_STAT, TigaseLoadBoshLastMinute);
		return TigaseLoadBoshLastMinute;
	}

}
