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

import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.RDN;
import com.unboundid.ldap.sdk.ResultCode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Simplified implementation of LDAP DN parser that supports only a single CN, UID, OU in a DN string.
 * Multiple DC attributes are allowed but will be returned as a domain name.
 */
public class DN {

	public static DN parse(String dn) throws LDAPException {
		String cn = null;
		String ou = null;
		String uid = null;
		StringBuilder domain = null;
		RDN[] rdns = com.unboundid.ldap.sdk.DN.getRDNs(dn);
		for (RDN rdn : rdns) {
			for (var pair : rdn.getNameValuePairs()) {
				switch (pair.getAttributeName()) {
					case "uid" -> {
						if (uid == null) {
							uid = pair.getAttributeValue();
						} else {
							throw new LDAPException(ResultCode.NOT_SUPPORTED);
						}
					}
					case "cn" -> {
						if (cn == null) {
							cn = pair.getAttributeValue();
						} else {
							throw new LDAPException(ResultCode.NOT_SUPPORTED);
						}
					}
					case "ou" -> {
						if (ou == null) {
							ou = pair.getAttributeValue();
						} else {
							throw new LDAPException(ResultCode.NOT_SUPPORTED);
						}
					}
					case "dc" -> {
						if (domain == null) {
							domain = new StringBuilder(pair.getAttributeValue());
						} else {
							domain.append(".").append(pair.getAttributeValue());
						}
					}
				}
			}
		}
		return new DN(cn, uid, ou, domain == null ? null : domain.toString());
	}

	private String cn;
	private String uid;
	private String ou;
	private String domain;
	
	public DN() {
		this(null, null, null, null);
	}

	public DN(String cn, String uid, String ou, String domain) {
		this.cn = cn;
		this.ou = ou;
		this.uid = uid;
		this.domain = domain;
	}

	public String getCn() {
		return cn;
	}

	public DN setCN(String cn) {
		this.cn = cn;
		return this;
	}

	public String getDomain() {
		return domain;
	}

	public DN setDomain(String domain) {
		this.domain = domain;
		return this;
	}

	public String getOu() {
		return ou;
	}

	public DN setOU(String ou) {
		this.ou = ou;
		return this;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public DN copy() {
		return new DN(cn, uid, ou, domain);
	}

	public com.unboundid.ldap.sdk.DN build() {
		List<RDN> rdns = new ArrayList<>();
		if (uid != null) {
			rdns.add(new RDN("uid", uid));
		}
		if (cn != null) {
			rdns.add(new RDN("cn", cn));
		}
		if (ou != null) {
			rdns.add(new RDN("ou", ou));
		}
		if (domain != null) {
			Arrays.stream(domain.split("\\.")).map(part -> new RDN("dc", part)).forEach(rdns::add);
		}
		return new com.unboundid.ldap.sdk.DN(rdns.toArray(new RDN[0]));
	}

	/**
	 * Serialize DN to a string representation
	 * @return DN in a string
	 */
	public String toString() {
		return build().toString();
	}
}
