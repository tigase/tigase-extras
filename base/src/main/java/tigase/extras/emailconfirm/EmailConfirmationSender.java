package tigase.extras.emailconfirm;

import tigase.component.exceptions.RepositoryException;
import tigase.db.AuthRepository;
import tigase.db.UserRepository;
import tigase.eventbus.EventBus;
import tigase.extras.AbstractEmailSender;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.core.Kernel;
import tigase.util.DNSResolverFactory;
import tigase.util.Token;
import tigase.xmpp.Authorization;
import tigase.xmpp.BareJID;
import tigase.xmpp.XMPPProcessorException;
import tigase.xmpp.impl.JabberIqRegister;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by bmalkow on 21.04.2017.
 */

@Bean(name = "account-registration-email-validator", parent = Kernel.class, active = false, exportable = true)
public class EmailConfirmationSender extends AbstractEmailSender
		implements JabberIqRegister.AccountValidator, Initializable, UnregisterAware {

	public static final String EMAIL_CONFIRMATION_TOKEN_KEY = "email-confirmation-token";
	protected final Logger log = Logger.getLogger(this.getClass().getName());
	@ConfigField(desc = "URL of token verifier")
	private String tokenVerifierURL = "http://" + DNSResolverFactory.getInstance().getDefaultHost() + ":8080/rest/user/confirm/";

	@Inject
	private EventBus eventBus;
	@Inject
	private AuthRepository authRepository;
	@Inject
	private UserRepository userRepository;

	public EmailConfirmationSender() {
		super("Email confirmation", "mails/email-confirmation.template");
	}

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
			sendMail(email, bindings);
		} catch (Exception e) {
			log.log(Level.WARNING, "Cannot send confirmation mail", e);
		}
	}

}

