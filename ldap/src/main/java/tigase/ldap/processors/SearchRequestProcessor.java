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
import com.unboundid.ldap.protocol.SearchRequestProtocolOp;
import com.unboundid.ldap.protocol.SearchResultDoneProtocolOp;
import com.unboundid.ldap.protocol.SearchResultEntryProtocolOp;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import tigase.db.TigaseDBException;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.config.ConfigField;
import tigase.ldap.LdapConnectionManager;
import tigase.ldap.utils.DN;
import tigase.ldap.utils.FilterHelper;
import tigase.ldap.utils.Group;
import tigase.ldap.utils.PermissionCheck;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xmpp.jid.BareJID;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static tigase.ldap.utils.FilterHelper.extractUserId;

@Bean(name = "searchRequest", active = true, parent = LdapConnectionManager.class)
public class SearchRequestProcessor extends AbstractLDAPProcessor<SearchRequestProtocolOp> {

	private static final Logger log = Logger.getLogger(SearchRequestProcessor.class.getCanonicalName());

	@ConfigField(desc = "Administrators group name", alias = "adminsGroupName", allowAliasFromParent = true)
	private String adminsGroupName = "Administrators";
	@ConfigField(desc = "Users group name", alias = "usersGroupName", allowAliasFromParent = true)
	private String usersGroupName = "Users";

	@Override
	public Class<SearchRequestProtocolOp> getSupportedProtocolOp() {
		return SearchRequestProtocolOp.class;
	}

	@Override
	public void process(LDAPSession session, SearchRequestProtocolOp searchRequest, Consumer<ProtocolOp> consumer) throws Exception {
		if (searchRequest.getBaseDN().isEmpty()) {
			consumer.accept(new SearchResultEntryProtocolOp(searchRequest.getBaseDN(), List.of(
			)));
			consumer.accept(new SearchResultDoneProtocolOp(ResultCode.SUCCESS_INT_VALUE, null, null, null));
			return;
		}

		try {

			checkAuthorization(session);

			DN dn;
			try {
				dn = DN.parse(searchRequest.getBaseDN());
			} catch (LDAPException e) {
				consumer.accept(new SearchResultDoneProtocolOp(e.toLDAPResult()));
				return;
			}

			log.finest(() -> "search request base DN: " + dn);

			Filter filter = searchRequest.getFilter();
			log.finest(() -> "search request filter: " + FilterHelper.printFilterTree(filter));

			switch (dn.getOu()) {
				case "Users" -> {
					processUserSearch(dn.getDomain(), filter, jid -> canAccessToUserData(session, jid), consumer);
				}
				case "Groups" -> {
					processGroupSearch(dn.getDomain(), dn.getCn(), filter, jid -> canAccessToUserData(session, jid),
									   consumer);
				}
				default -> consumer.accept(
						new SearchResultDoneProtocolOp(ResultCode.NOT_SUPPORTED_INT_VALUE, null, null, null));
			}
		} catch (LDAPException e) {
			consumer.accept(new SearchResultDoneProtocolOp(e.toLDAPResult()));
		}
	}

	/**
	 * Method executes group search with passed parameters
	 * @param domain domain to look into
	 * @param groupName name of the group to look for
	 * @param filter filter to look for groups
	 * @param permissionCheck method to check if authorized user can access group data
	 * @param consumer method to return result
	 * @throws LDAPException is thrown if unrecoverable error will happen
	 */
	private void processGroupSearch(String domain, String groupName, Filter filter, PermissionCheck permissionCheck, Consumer<ProtocolOp> consumer) throws LDAPException {
		List<Group> groups = getGroups(groupName == null ? null : group -> group.name().equals(groupName)).filter(
				group -> FilterHelper.testGroup(domain, group, filter, permissionCheck)).toList();

		DN groupDN = new DN();
		groupDN.setDomain(domain);
		groupDN.setOU("Groups");
		for (Group group : groups) {
			var attrs = List.of(
					new Attribute("cn", group.name()),
					new Attribute("objectclass", "posixGroup")
			);
			consumer.accept(new SearchResultEntryProtocolOp(groupDN.setCN(group.name()).toString(), attrs));
		}
		consumer.accept(new SearchResultDoneProtocolOp(ResultCode.SUCCESS_INT_VALUE, null, null, null));
	}

	/**
	 * Method returns groups returned by getAllGroups() filtered by passed predicate
	 * @param predicate - to filter or null to skip filtering
	 * @return Stream of groups matching predicate
	 */
	private Stream<Group> getGroups(Predicate<Group> predicate) {
		Stream<Group> stream = getAllGroups().stream();
		if (predicate != null) {
			stream = stream.filter(predicate);
		}
		return stream;
	}

	/**
	 * Method searches for the user matching filter in domain and returns result
	 * @param domain domain to look into
	 * @param filter filter to look for users
	 * @param permissionCheck method to check if authorized user can access found user data
	 * @param consumer method to return result
	 * @throws LDAPException is thrown if unrecoverable error will happen
	 */
	private void processUserSearch(String domain, Filter filter, PermissionCheck permissionCheck, Consumer<ProtocolOp> consumer) throws LDAPException {
		try {
			BareJID userJid = findUser(permissionCheck, domain, filter);
			if (userJid != null) {
				var userGroupNames = getAllGroups().stream()
						.filter(group -> group.membershipPredicate().test(userJid))
						.map(Group::name)
						.toList();

				DN domainDN = new DN();
				domainDN.setDomain(domain);

				String userDN = domainDN.copy().setOU("Users").setCN(userJid.getLocalpart()).toString();
				DN groupDN = domainDN.copy().setOU("Groups");

				var attrs = List.of(new Attribute("uid", userJid.getLocalpart()),
									new Attribute("cn", userJid.getLocalpart()),
									new Attribute("objectClass", "posixAccount"),
									new Attribute("mail", userJid.toString()),
									new Attribute("xmpp", userJid.toString()),
									new Attribute("memberOfGid", userGroupNames), new Attribute("memberOf",
																								userGroupNames.stream()
																										.map(cn -> groupDN.setCN(
																														cn)
																												.toString())
																										.toList()),
									new Attribute("accountStatus",
												  getAuthRepository().getAccountStatus(userJid).name()));

				consumer.accept(new SearchResultEntryProtocolOp(userDN, attrs));
			}
			consumer.accept(new SearchResultDoneProtocolOp(ResultCode.SUCCESS_INT_VALUE, null, null, null));
		} catch (TigaseDBException ex) {
			throw new LDAPException(ResultCode.BUSY, ex);
		}
	}

	/**
	 * Method looks for the user in the filter and checks if belongs to the domain and matches filter
	 * @param permissionCheck method to check if authorized user can access found user data
	 * @param domain domain to look into
	 * @param filter filter to look for users
	 * @return user jid or null if user was not found
	 * @throws LDAPException is thrown if permission check fails for data is malformed or not supported
	 */
	private BareJID findUser(PermissionCheck permissionCheck, String domain, Filter filter) throws LDAPException {
		String userId = extractUserId(filter);
		log.finest(() -> "found user id " + userId + " for domain " + domain + " in filter...");
		if (userId == null) {
			return null;
		}

		try {
			// we are assuming that either user id is passed (localpart) or the whole bare JID
			BareJID jid = userId.endsWith("@" + domain)
						  ? BareJID.bareJIDInstance(userId)
						  : BareJID.bareJIDInstance(userId, domain);
			permissionCheck.checkPermissionToAccess(jid);

			if (!getUserRepository().userExists(jid)) {
				log.finest(() -> "user " + jid + " not found in the user repository!");
				return null;
			}
			if (!FilterHelper.testUser(jid, filter, getAuthRepository(), groupName -> {
				Group group = getGroupByName(groupName);
				if (group == null) {
					return false;
				}
				return group.membershipPredicate().test(jid);
			})) {
				log.finest(() -> "user " + jid + " do not match passed filter " + FilterHelper.printFilterTree(filter));
				return null;
			}
			return jid;
		} catch (TigaseStringprepException ex) {
			// most likely incorrect localpart, we should assume processing has failed
			return null;
		}
	}

	/**
	 * Find group from getAllGroups() that has the name matching parameter
	 * @param name name to compare with group name
	 * @return group with matching name
	 */
	private Group getGroupByName(String name) {
		return getAllGroups().stream().filter(group -> group.name().equals(name)).findFirst().orElse(null);
	}

	/**
	 * Returns a list of known groups
	 * @return list of known groups
	 */
	private List<Group> getAllGroups() {
		return List.of(
				new Group(adminsGroupName, this::isAdmin),
				new Group(usersGroupName, jid -> getUserRepository().userExists(jid))
		);
	}

}
