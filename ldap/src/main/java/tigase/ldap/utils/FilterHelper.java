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
package tigase.ldap.utils;

import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import tigase.db.AuthRepository;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xmpp.jid.BareJID;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class contains static helper methods used for searching/filtering.
 */
public class FilterHelper {

	private static Logger log = Logger.getLogger(FilterHelper.class.getCanonicalName());

	public static String extractUserId(Filter filter) {
		return switch (filter.getFilterType()) {
			case Filter.FILTER_TYPE_EQUALITY -> switch (filter.getAttributeName().toLowerCase()) {
				case "uid", "cn" -> filter.getAssertionValue();
				case "mail", "xmpp" -> filter.getAssertionValue();
				default -> null;
			};
			// with both conditions we require that both values sent to us need to be equal as only one JID or localpart is allowed
			case Filter.FILTER_TYPE_AND, Filter.FILTER_TYPE_OR  -> {
				Set<String> values = new HashSet<>();
				for (Filter component : filter.getComponents()) {
					String value = extractUserId(component);
					if (value != null) {
						values.add(value);
					}
				}
				yield switch (values.size()) {
					case 0 -> null;
					case 1 -> values.iterator().next();
					default -> throw new UnsupportedOperationException();
				};
			}
			default -> null;
		};
	}

	public static boolean testUser(BareJID jid, Filter filter, AuthRepository authRepository, Predicate<String> testGroupMembership) throws Exception {
		var result = switch (filter.getFilterType()) {
			case Filter.FILTER_TYPE_EQUALITY -> switch (filter.getAttributeName().toLowerCase()) {
				case "uid", "cn" -> Objects.equals(jid.getLocalpart(), filter.getAssertionValue());
				case "mail" -> Objects.equals(jid.toString(), filter.getAssertionValue());
				case "xmpp" -> Objects.equals(jid.toString(), filter.getAssertionValue());
				case "memberof" -> {
					DN groupDN = DN.parse(filter.getAssertionValue());
					if (!Objects.equals(groupDN.getDomain(), jid.getDomain())) {
						yield false;
					}
					yield testGroupMembership.test(groupDN.getCn());
				}
				case "memberofgid" -> testGroupMembership.test(filter.getAssertionValue());
				case "objectclass" -> "posixAccount".equals(filter.getAssertionValue());
				case "accountstatus" -> Objects.equals(authRepository.getAccountStatus(jid).name(), filter.getAssertionValue());
				default -> false;
			};
			case Filter.FILTER_TYPE_PRESENCE -> switch (filter.getAttributeName().toLowerCase()) {
				case "uid", "cn", "mail", "xmpp", "memberof", "memberofgid", "objectclass", "accountstatus" -> true;
				default -> false;
			};
			case Filter.FILTER_TYPE_AND -> {
				for (Filter child : filter.getComponents()) {
					if (!testUser(jid, child, authRepository, testGroupMembership)) {
						yield  false;
					}
				}
				yield true;
			}
			case Filter.FILTER_TYPE_OR -> {
				for (Filter child : filter.getComponents()) {
					if (testUser(jid, child, authRepository, testGroupMembership)) {
						yield true;
					}
				}
				yield false;
			}
			default -> false;
		};
		log.finest(() -> "testing user " + jid + " using filter " + filter + " with result " + result);
		return result;
	}
	
	public static boolean testGroup(String domain, Group group, Filter filter, PermissionCheck permissionCheck) {
		return switch (filter.getFilterType()) {
			case Filter.FILTER_TYPE_EQUALITY -> switch (filter.getAttributeName().toLowerCase()) {
				case "objectclass" -> "posixGroup".equals(filter.getAssertionValue());
				case "cn" -> Objects.equals(group.name(), filter.getAssertionValue());
				case "memberuid" -> {
					try {
						BareJID jid = BareJID.bareJIDInstance(filter.getAssertionValue(), domain);
						yield permissionCheck.canAccess(jid) && group.membershipPredicate().test(jid);
					} catch (TigaseStringprepException ex) {
						yield false;
					}
				}
				case "member" -> {
					try {
						DN memberDN = DN.parse(filter.getAssertionValue());
						String localPart = memberDN.getUid() != null ? memberDN.getUid() : memberDN.getCn();
						if (!memberDN.getDomain().equals(domain)) {
							yield false;
						}
						BareJID jid = BareJID.bareJIDInstance(localPart, domain);
						yield permissionCheck.canAccess(jid) && group.membershipPredicate().test(jid);
					} catch (LDAPException | TigaseStringprepException ex) {
						log.fine("Invalid group DN " + filter.getAssertionValue());
						yield false;
					}
				}
				default -> false;
			};
			case Filter.FILTER_TYPE_PRESENCE -> switch (filter.getAttributeName().toLowerCase()) {
				case "objectclass", "cn", "member", "memberuid" -> true;
				default -> false;
			};
			case Filter.FILTER_TYPE_AND -> {
				for (Filter child : filter.getComponents()) {
					if (!testGroup(domain, group, child, permissionCheck)) {
						yield  false;
					}
				}
				yield true;
			}
			case Filter.FILTER_TYPE_OR -> {
				for (Filter child : filter.getComponents()) {
					if (testGroup(domain, group, child, permissionCheck)) {
						yield true;
					}
				}
				yield false;
			}
			default -> false;
		};
	}

	public static String printFilterTree(Filter filter) {
		StringBuilder sb = new StringBuilder();
		printFilterTree(filter, sb, 0);
		return sb.toString();
	}

	private static void printFilterTree(Filter filter, StringBuilder sb, int level) {
		String prefix = "\n" + Stream.generate(() -> " ").limit(level * 4).collect(Collectors.joining());
		switch (filter.getFilterType()) {
			case Filter.FILTER_TYPE_AND -> {
				sb.append(prefix);
				sb.append("AND(");
				for (Filter f : filter.getComponents()) {
					printFilterTree(f, sb, level + 1);
				}
				sb.append(prefix);
				sb.append(")");
			}
			case Filter.FILTER_TYPE_OR -> {
				sb.append(prefix);
				sb.append("OR(");
				for (Filter f : filter.getComponents()) {
					printFilterTree(f, sb, level + 1);
				}
				sb.append(prefix);
				sb.append(")");
			}
			case Filter.FILTER_TYPE_NOT -> {
				sb.append("NOT(");
				printFilterTree(filter.getNOTComponent(), sb, level + 1);
				sb.append(")");
			}
			case Filter.FILTER_TYPE_EQUALITY -> {
				sb.append(prefix);
				sb.append(filter.getAttributeName());
				sb.append(" = ");
				sb.append(filter.getAssertionValue());
			}
		}
	}
}
