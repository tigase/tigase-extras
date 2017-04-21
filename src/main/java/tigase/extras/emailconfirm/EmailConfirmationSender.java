package tigase.extras.emailconfirm;

import groovy.lang.Writable;
import groovy.text.SimpleTemplateEngine;
import groovy.text.Template;
import tigase.db.UserRepository;
import tigase.eventbus.EventBus;
import tigase.extras.mailer.Mailer;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;
import tigase.xmpp.BareJID;
import tigase.xmpp.impl.JabberIqRegister;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by bmalkow on 21.04.2017.
 */

@Bean(name = "EmailConfirmationSender", active = true, exportable = true)
public class EmailConfirmationSender
		implements JabberIqRegister.MailConfirmationTokenSender, Initializable, UnregisterAware {

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
	private String tokenVerifierURL = "http://token.verifier.com/token=";
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

	@Override
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

