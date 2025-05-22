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

import com.unboundid.ldap.protocol.BindRequestProtocolOp;
import com.unboundid.ldap.protocol.BindResponseProtocolOp;
import com.unboundid.ldap.protocol.ProtocolOp;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import tigase.auth.TigaseSaslProvider;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.ldap.LdapConnectionManager;
import tigase.ldap.utils.DN;
import tigase.server.xmppsession.SessionManagerHandler;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.vhosts.VHostItemImpl;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Class implements support for LDAP simple bind request used for authentication.
 */
@Bean(name = "bindRequest", active = true, parent = LdapConnectionManager.class)
public class BindRequestProcessor extends AbstractLDAPProcessor<BindRequestProtocolOp> {

	private static final Logger log = Logger.getLogger(BindRequestProcessor.class.getCanonicalName());

	private final DummySessionManagerHandler sessionManagerHandler = new DummySessionManagerHandler();

	@Inject
	private LdapConnectionManager connectionManager;
	@Inject
	private TigaseSaslProvider saslProvider;

	@Override
	public boolean canHandle(ProtocolOp request) {
		return super.canHandle(request) && ((BindRequestProtocolOp) request).getSimplePassword() != null;
	}

	@Override
	public Class<BindRequestProtocolOp> getSupportedProtocolOp() {
		return BindRequestProtocolOp.class;
	}

	@Override
	public void process(LDAPSession session, BindRequestProtocolOp request, Consumer<ProtocolOp> consumer)
			throws Exception {
		if (request.getBindDN().isEmpty() && request.getSimplePassword().stringValue().isEmpty()) {
			consumer.accept(new BindResponseProtocolOp(ResultCode.SUCCESS_INT_VALUE, null, null, null, null));
			return;
		}

		try {
			DN userDN = DN.parse(request.getBindDN());
			String localPart = userDN.getUid() == null ? userDN.getCn() : userDN.getUid();
			String domain = userDN.getDomain();

			XMPPResourceConnection connection = createConnection(domain);
			CallbackHandler handler = saslProvider.create("PLAIN", connection, null, new HashMap<>());
			SaslServer ss = Sasl.createSaslServer("PLAIN", "xmpp", domain, new HashMap<>(), handler);
			BareJID jid = BareJID.bareJIDInstance(localPart, domain);
			byte[] password = request.getSimplePassword().getValue();

			log.finest(() -> "authenticating jid " + jid + "...");
			ss.evaluateResponse(generatePlain(jid, password));
			if (ss.isComplete()) {
				session.setAuthorizedJID(jid);
				consumer.accept(new BindResponseProtocolOp(ResultCode.SUCCESS_INT_VALUE, null, null, null, null));
			} else {
				consumer.accept(
						new BindResponseProtocolOp(ResultCode.AUTHORIZATION_DENIED_INT_VALUE, null, null, null, null));
			}
		} catch (LDAPException|SaslException|TigaseStringprepException ex) {
			consumer.accept(new BindResponseProtocolOp(ResultCode.INVALID_CREDENTIALS_INT_VALUE, ex.getMessage(), null, null, null));
		}
	}

	private XMPPResourceConnection createConnection(String domain) throws TigaseStringprepException {
		XMPPResourceConnection connection = new XMPPResourceConnection(
				connectionManager.getComponentId().copyWithResource(UUID.randomUUID().toString()), getUserRepository(), getAuthRepository(),
				sessionManagerHandler);
		connection.setDomain(new VHostItemImpl(JID.jidInstance(domain)));
		return connection;
	}

	private byte[] generatePlain(BareJID jid, byte[] password) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		baos.write(0x00);
		baos.write(jid.toString().getBytes(StandardCharsets.UTF_8));
		baos.write(0x00);
		baos.write(password);
		return baos.toByteArray();
	}

	private static class DummySessionManagerHandler implements SessionManagerHandler {
		@Override
		public JID getComponentId() {
			return null;
		}

		@Override
		public void handleLogin(BareJID userId, XMPPResourceConnection conn) {

		}

		@Override
		public void handleDomainChange(String domain, XMPPResourceConnection conn) {

		}

		@Override
		public void handleLogout(BareJID userId, XMPPResourceConnection conn) {

		}

		@Override
		public void handlePresenceSet(XMPPResourceConnection conn) {

		}

		@Override
		public void handleResourceBind(XMPPResourceConnection conn) {

		}

		@Override
		public boolean isLocalDomain(String domain, boolean includeComponents) {
			return false;
		}
	}
}
