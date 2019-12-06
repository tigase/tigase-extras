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

//
// Generated by mibgen version 5.1 (03/08/07) when compiling TIGASE-MANAGEMENT-MIB in standard metadata mode.
//

// jmx imports
//

import com.sun.management.snmp.SnmpStatusException;

/**
 * This interface is used for representing the remote management interface for the "TigaseUser" MBean.
 */
public interface TigaseUserMBean {

	public Long getTigaseUserRegisteredCount() throws SnmpStatusException;

	public Long getTigaseUserSessionCount() throws SnmpStatusException;

}
