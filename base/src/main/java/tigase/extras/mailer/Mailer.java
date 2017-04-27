package tigase.extras.mailer;

import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.config.ConfigField;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by bmalkow on 21.04.2017.
 */
@Bean(name = "mailer", active = true, exportable = true)
public class Mailer
		implements Initializable {

	protected final Logger log = Logger.getLogger(this.getClass().getName());
	@ConfigField(desc = "Default sender address (field 'from'")
	private String fromAddress;
	private Session session;
	@ConfigField(desc = "SMTP host")
	private String smtpHost;
	@ConfigField(desc = "SMTP password")
	private String smtpPassword;
	@ConfigField(desc = "SMTP port")
	private String smtpPort = "587";
	@ConfigField(desc = "SMTP username")
	private String smtpUsername;

	@Override
	public void initialize() {
		log.config("Configuring Mailer...");
		Properties sessionProperties = new Properties();

		sessionProperties.put("mail.smtp.auth", "false");
		sessionProperties.put("mail.smtp.starttls.enable", "true");
		sessionProperties.put("mail.smtp.ssl.trust", "*");
		sessionProperties.put("mail.smtps.ssl.trust", "*");

		sessionProperties.put("mail.smtp.ssl.checkserveridentity", "false");
		sessionProperties.put("mail.smtp.ssl.trust", "*");
		sessionProperties.put("mail.smtp.starttls.checkserveridentity", "false");
		sessionProperties.put("mail.smtp.starttls.trust", "*");

		try {
			sessionProperties.put("mail.smtp.host", smtpHost);
			sessionProperties.put("mail.smtp.port", smtpPort);

			log.log(Level.CONFIG, "Setting username: {0}", smtpUsername);
			if (smtpPassword != null && !smtpPassword.isEmpty()) {
				log.log(Level.CONFIG, "Setting password: {0}", smtpPassword.replaceAll(".", "*"));
			}

			log.log(Level.CONFIG, "Setting default fromAddress: {0}", fromAddress);

			this.session = Session.getInstance(sessionProperties);
			log.log(Level.CONFIG, "Setting session: {0}", session);
		} catch (RuntimeException e) {
			log.warning(e.getMessage());
			log.warning("Mailer is not started");
			return;
		}

		log.config("Mailer is configured");
	}

	public void sendMail(String toAddresses, String messageSubject, String messageText) throws MailerException {
		sendMail(null, toAddresses, messageSubject, messageText);
	}

	public void sendMail(String from, String toAddresses, String messageSubject, String messageText)
			throws MailerException {
		try {
			log.fine("Sending mail: " + messageText);

			Message message = new MimeMessage(session);
			if (from == null) {
				message.setFrom(new InternetAddress(fromAddress));
			} else {
				message.setFrom(new InternetAddress(from));
			}

			InternetAddress[] to = InternetAddress.parse(toAddresses);
			message.setRecipients(Message.RecipientType.TO, to);
			message.setSubject(messageSubject);
			message.setText(messageText);

			if (smtpUsername == null || smtpUsername.trim().isEmpty()) {
				Transport.send(message, to);
			} else {
				Transport.send(message, to, smtpUsername, smtpPassword);
			}
		} catch (Exception e) {
			log.log(Level.WARNING, "Can't send mail", e);
			throw new MailerException(e);
		}
	}
}
