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
package tigase.ldap.utils;

import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import tigase.xmpp.jid.BareJID;

public interface PermissionCheck {

	boolean canAccess(BareJID jid);

	default void checkPermissionToAccess(BareJID jid) throws LDAPException {
		if (!canAccess(jid)) {
			throw new LDAPException(ResultCode.AUTHORIZATION_DENIED, "You do not have permission to access this user data!");
		}
	}

}
