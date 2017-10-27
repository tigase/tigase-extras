/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2017 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 */

package tigase.extras.passwordreset;

import tigase.auth.PasswordResetterIfc;
import tigase.auth.credentials.Credentials;
import tigase.component.exceptions.RepositoryException;
import tigase.db.AuthRepository;
import tigase.db.UserRepository;
import tigase.extras.AbstractEmailSender;
import tigase.extras.mailer.Mailer;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.core.Kernel;
import tigase.util.Token;
import tigase.xmpp.jid.BareJID;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@Bean(name = "account-email-password-resetter", parent = Kernel.class, active = false, exportable = true)
public class EmailPasswordResetter extends AbstractEmailSender implements PasswordResetterIfc {

	private static final Logger log = Logger.getLogger(EmailPasswordResetter.class.getCanonicalName());

	public static final String EMAIL_PASSWORD_RESET_TOKEN = "email-password-reset-token";

	@Inject
	private Mailer mailer;
	@Inject
	private UserRepository userRepository;
	@Inject
	private AuthRepository authRepository;

	@ConfigField(desc = "URL of token verifier")
	private String tokenVerifierURL;

	public EmailPasswordResetter() {
		super("Password reset", "mails/email-password-reset.template");
	}

	@Override
	public void validateToken(String encodedToken) throws RepositoryException {
		Token token = Token.parse(encodedToken);
		String tokenHash = userRepository.getData(token.getJid(), EMAIL_PASSWORD_RESET_TOKEN);
		if (tokenHash == null) {
			throw new RuntimeException("Invalid token");
		}
		if (!token.getHash().equals(tokenHash)) {
			throw new RuntimeException("Invalid token");
		}
	}

	@Override
	public void changePassword(String encodedToken, String password) throws RepositoryException {
		Token token = Token.parse(encodedToken);
		authRepository.updateCredential(token.getJid(), Credentials.DEFAULT_USERNAME, password);
		userRepository.removeData(token.getJid(), EMAIL_PASSWORD_RESET_TOKEN);
	}

	@Override
	public void sendToken(BareJID bareJID, String url) throws RepositoryException, Exception {
		Token token = Token.create(bareJID);

		String email = userRepository.getData(bareJID, "email");
		userRepository.setData(bareJID, EMAIL_PASSWORD_RESET_TOKEN, token.getHash());

		Map<String, Object> bindings = new HashMap<>();
		bindings.put("jid", bareJID);
		bindings.put("token", token);
		bindings.put("tokenEncoded", token.getEncoded());
		bindings.put("tokenVerifierURL", tokenVerifierURL == null ? url : tokenVerifierURL);

		sendMail(email, bindings);
	}

}
