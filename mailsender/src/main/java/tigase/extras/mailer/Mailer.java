/*
 * Tigase Server Extras MailSender - Extra modules to Tigase Server
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
package tigase.extras.mailer;

import com.sun.mail.smtp.SMTPAddressFailedException;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.config.ConfigField;
import tigase.util.StringUtilities;

import javax.mail.Message;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
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
	@ConfigField(desc = "Default sender address (field 'from'", alias = "mailer-from-address")
	private String fromAddress;
	private Session session;
	@ConfigField(desc = "SMTP host", alias = "mailer-smtp-host")
	private String smtpHost;
	@ConfigField(desc = "SMTP password", alias = "mailer-smtp-password")
	private String smtpPassword;
	@ConfigField(desc = "SMTP port", alias = "mailer-smtp-port")
	private String smtpPort = "587";
	@ConfigField(desc = "SMTP username", alias = "mailer-smtp-username")
	private String smtpUsername;

	public void setFromAddress(String fromAddress) {
		try {
			final InternetAddress internetAddress = new InternetAddress(fromAddress);
			internetAddress.validate();
		} catch (AddressException e) {
			log.log(Level.SEVERE, "Address: " + fromAddress + " is invalid: " + e);
		}
		this.fromAddress = fromAddress;
	}

	@Override
	public void initialize() {
		log.config("Configuring Mailer...");
		Properties sessionProperties = new Properties();

		sessionProperties.put("mail.smtp.auth", "false");
		sessionProperties.put("mail.smtp.starttls.enable", "true");
		sessionProperties.put("mail.smtp.ssl.trust", "*");
		sessionProperties.put("mail.smtps.ssl.trust", "*");

		sessionProperties.put("mail.smtp.timeout", 5000);

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
			log.log(Level.WARNING, "Mailer is not started", e);
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
			if (log.isLoggable(Level.FINE)) {
				final String message = messageText != null ? StringUtilities.convertNonPrintableCharactersToLiterals(
						messageText.substring(0, Math.min(messageText.length(), 2048)) + " ...") : null;
				log.log(Level.FINE, "Sending mail, to: {0}, subject: {1}, message: {2}",
						new Object[]{toAddresses, messageSubject, message});
			}

			Message message = new MimeMessage(session);
			if (from == null) {
				message.setFrom(new InternetAddress(fromAddress));
			} else {
				message.setFrom(new InternetAddress(from));
			}

			InternetAddress[] to = InternetAddress.parse(toAddresses);
			for (InternetAddress address : to) {
				address.validate();
			}
			message.setRecipients(Message.RecipientType.TO, to);
			message.setSubject(messageSubject);
			message.setText(messageText);

			if (smtpUsername == null || smtpUsername.trim().isEmpty()) {
				Transport.send(message, to);
			} else {
				Transport.send(message, to, smtpUsername, smtpPassword);
			}
		} catch (AddressException | SMTPAddressFailedException e) {
			handleException(Level.FINE, toAddresses, messageSubject, messageText, e);
		} catch (SendFailedException e) {
			handleException(Level.INFO, toAddresses, messageSubject, messageText, e);
		} catch (Exception e) {
			handleException(Level.WARNING, toAddresses, messageSubject, messageText, e);
		}
	}

	private void handleException(Level logLevel, String toAddresses, String messageSubject, String messageText,
								 Exception e) {
		log.log(logLevel, "Can''t send mail to: " + toAddresses + ", subject: " + messageSubject + ", text size: " +
				(messageText != null ? messageText.length() : null) + ", exception: " + e);
		throw new MailerException(e);
	}
}
