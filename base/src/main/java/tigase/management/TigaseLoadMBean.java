/*
 * TigaseLoadMBean.java
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

/**
 * This interface is used for representing the remote management interface for the "TigaseLoad" MBean.
 */
public interface TigaseLoadMBean {

	public Long getTigaseLoadBoshLastSecond() throws SnmpStatusException;

	public Long getTigaseLoadC2SLastHour() throws SnmpStatusException;

	public Long getTigaseLoadC2SLastMinute() throws SnmpStatusException;

	public Long getTigaseLoadC2SLastSecond() throws SnmpStatusException;

	public Long getTigaseLoadS2SLastHour() throws SnmpStatusException;

	public Long getTigaseLoadS2SLastMinute() throws SnmpStatusException;

	public Long getTigaseLoadS2SLastSecond() throws SnmpStatusException;

	public Long getTigaseLoadPubSubLastHour() throws SnmpStatusException;

	public Long getTigaseLoadPubSubLastMinute() throws SnmpStatusException;

	public Long getTigaseLoadPubSubLastSecond() throws SnmpStatusException;

	public Long getTigaseLoadMUCLastHour() throws SnmpStatusException;

	public Long getTigaseLoadMUCLastMinute() throws SnmpStatusException;

	public Long getTigaseLoadMUCLastSecond() throws SnmpStatusException;

	public Long getTigaseLoadMRLastHour() throws SnmpStatusException;

	public Long getTigaseLoadMRLastMinute() throws SnmpStatusException;

	public Long getTigaseLoadMRLastSecond() throws SnmpStatusException;

	public Long getTigaseLoadSMLastHour() throws SnmpStatusException;

	public Long getTigaseLoadSMLastMinute() throws SnmpStatusException;

	public Long getTigaseLoadSMLastSecond() throws SnmpStatusException;

	public Long getTigaseLoadBoshLastHour() throws SnmpStatusException;

	public Long getTigaseLoadBoshLastMinute() throws SnmpStatusException;

}
