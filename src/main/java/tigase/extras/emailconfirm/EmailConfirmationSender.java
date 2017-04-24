package tigase.extras.emailconfirm;

import groovy.lang.Writable;
import groovy.text.SimpleTemplateEngine;
import groovy.text.Template;
import tigase.component.exceptions.RepositoryException;
import tigase.db.AuthRepository;
import tigase.db.UserRepository;
import tigase.eventbus.EventBus;
import tigase.extras.mailer.Mailer;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.core.Kernel;
import tigase.util.DNSResolverFactory;
import tigase.xmpp.Authorization;
import tigase.xmpp.BareJID;
import tigase.xmpp.XMPPProcessorException;
import tigase.xmpp.impl.JabberIqRegister;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by bmalkow on 21.04.2017.
 */

@Bean(name = "account-registration-email-validator", parent = Kernel.class, active = false, exportable = true)
public class EmailConfirmationSender
		implements JabberIqRegister.AccountValidator, Initializable, UnregisterAware {

	public static final String EMAIL_CONFIRMATION_TOKEN_KEY = "email-confirmation-token";
	protected final Logger log = Logger.getLogger(this.getClass().getName());
	@Inject
	private EventBus eventBus;
	@Inject
	private Mailer mailer;
	private SimpleTemplateEngine ste = new SimpleTemplateEngine();
	@ConfigField(desc = "Notification email subject")
	private String subject = "Email confirmation";
	@ConfigField(desc = "Email template file")
	private String templateFile = "mails/email-confirmation.template";
	@ConfigField(desc = "URL of token verifier")
	private String tokenVerifierURL = "http://" + DNSResolverFactory.getInstance().getDefaultHost() + ":8080/rest/user/confirm/";
	@Inject
	private AuthRepository authRepository;
	@Inject
	private UserRepository userRepository;

	@Override
	public void beforeUnregister() {
		eventBus.unregisterAll(this);
	}

	@Override
	public void initialize() {
		eventBus.registerAll(this);
	}

	@Override
	public void checkRequiredParameters(BareJID jid, Map<String, String> reg_params) throws XMPPProcessorException {
		if (reg_params.getOrDefault("email", "").trim().isEmpty()) {
			throw new XMPPProcessorException(Authorization.NOT_ACCEPTABLE, "Email address is required");
		}
	}

	@Override
	public boolean sendAccountValidation(BareJID jid, Map<String, String> reg_params) {
		String email = reg_params.get("email");
		sendToken(jid, email, reg_params);
		return true;
	}

	@Override
	public BareJID validateAccount(String encodedToken) {
		try {
			Token token = Token.parse(encodedToken);
			String tokenHash = userRepository.getData(token.getJid(), EMAIL_CONFIRMATION_TOKEN_KEY);
			if (tokenHash == null) {
				throw new RuntimeException("Invalid token");
			}
			if (!token.getHash().equals(tokenHash)) {
				throw new RuntimeException("Invalid token");
			}
			authRepository.setAccountStatus(token.getJid(), AuthRepository.AccountStatus.active);
			userRepository.removeData(token.getJid(), EMAIL_CONFIRMATION_TOKEN_KEY);
			return token.getJid();
		} catch (RepositoryException ex) {
			throw new RuntimeException("Internal Server Error", ex);
		}
	}

	private String load(final String file) throws IOException {
		File f = new File(file);
		InputStream is = null;
		if (f.exists()) {
			is = new FileInputStream(new File(file));
		} else {
			is = getClass().getResourceAsStream("/" + file);
		}
		if (is == null) {
			throw new IOException("Resource not found");
		}

		char[] buf = new char[1024];
		StringBuilder sb = new StringBuilder();
		Reader r = new InputStreamReader(is);
		try {
			int read;
			while ((read = r.read(buf)) > -1) {
				sb.append(buf, 0, read);
			}
		} finally {
			r.close();
		}
		return sb.toString();
	}
	
	public void sendToken(BareJID bareJID, String email, Map<String, String> req_params) {

		Token token = Token.create(bareJID);

		try {
			userRepository.setData(bareJID, EMAIL_CONFIRMATION_TOKEN_KEY, token.getHash());
		} catch (Exception e) {
			log.log(Level.WARNING, "Cannot store token in database", e);
			throw new RuntimeException("Cannot store token in database", e);
		}

		Map<String, Object> bindings = new HashMap<>();
		bindings.put("jid", bareJID);
		bindings.put("token", token);
		bindings.put("tokenEncoded", token.getEncoded());
		bindings.put("tokenVerifierURL", tokenVerifierURL);

		try {
			Template template = ste.createTemplate(load(templateFile));
			Writable w = template.make(bindings);
			mailer.sendMail(email, subject, w.toString());
		} catch (Exception e) {
			log.log(Level.WARNING, "Cannot send confirmation mail", e);
		}
	}

}

