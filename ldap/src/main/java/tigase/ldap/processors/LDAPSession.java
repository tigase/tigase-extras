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
package tigase.ldap.processors;

import tigase.xmpp.jid.BareJID;

public interface LDAPSession {

//	sealed interface Authorization permits Authorization.Anonymous, Authorization.User {
//		Authorization ANONYMOUS = new Anonymous();
//
//		final class Anonymous implements Authorization {}
//		record User(BareJID jid) implements Authorization {}
//	}
//
//	Authorization getAuthorization();

	BareJID getAuthorizedJID();

	void setAuthorizedJID(BareJID jid);

}
