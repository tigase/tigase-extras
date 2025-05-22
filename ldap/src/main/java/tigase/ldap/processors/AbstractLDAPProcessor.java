/*
 * Tigase Server Extras LDAP Server - Extra modules to Tigase Server
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

import com.unboundid.ldap.protocol.ProtocolOp;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import tigase.db.AuthRepository;
import tigase.db.UserRepository;
import tigase.kernel.beans.Inject;
import tigase.ldap.LdapConnectionManager;
import tigase.vhosts.VHostItem;
import tigase.vhosts.VHostManagerIfc;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

/**
 * Class contains helper methods often used by LDAPProcessor implementations.
 * @param <T> Class of the request that is supported
 */
public abstract class AbstractLDAPProcessor<T extends ProtocolOp> implements LDAPProcessor<T> {

	@Inject
	private AuthRepository authRepository;
	@Inject
	private UserRepository userRepository;
	@Inject
	private VHostManagerIfc vHostManager;
	@Inject
	private LdapConnectionManager connectionManager;

	protected AuthRepository getAuthRepository() {
		return authRepository;
	}

	protected UserRepository getUserRepository() {
		return userRepository;
	}

	protected void checkAuthorization(LDAPSession session) throws LDAPException {
		if (connectionManager.isAnonymousAccessAllowed()) {
			return;
		}
		if (session.getAuthorizedJID() == null) {
			throw new LDAPException(ResultCode.AUTH_UNKNOWN, "Not authenticated!");
		}
	}

	protected boolean canAccessToUserData(LDAPSession session, BareJID user) {
		if (connectionManager.isAnonymousAccessAllowed()) {
			return true;
		}
		BareJID authorizedJID = session.getAuthorizedJID();
		if (authorizedJID != null) {
			if (connectionManager.isAnyoneCanQuery()) {
				return true;
			}
			if (isAdmin(authorizedJID)) {
				return true;
			}
			return authorizedJID.equals(user);
		}
		return false;
	}

	protected boolean isAdmin(BareJID jid) {
		if (connectionManager.isAdmin(JID.jidInstance(jid))) {
			return true;
		}
		VHostItem item = vHostManager.getVHostItem(jid.getDomain());
		if (item == null) {
			return false;
		}
		String[] admins = item.getAdmins();
		if (admins == null || admins.length == 0) {
			return false;
		}
		String jidStr = jid.toString();
		for (String admin : admins) {
			if (admin.equals(jidStr)) {
				return true;
			}
		}
		return false;
	}
	
}
