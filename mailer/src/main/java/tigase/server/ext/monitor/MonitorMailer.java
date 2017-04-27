package tigase.server.ext.monitor;

import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import tigase.disteventbus.EventBus;
import tigase.disteventbus.EventHandler;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.monitor.MonitorComponent;
import tigase.monitor.MonitorExtension;
import tigase.xml.Element;

@Bean(name = MonitorMailer.ID)
public class MonitorMailer implements MonitorExtension, Initializable {

	public static final String ID = "monitor-mailer";
	private final static String REMOTE_EVENT_INDICATOR = "remote";
	protected final Logger log = Logger.getLogger(this.getClass().getName());
	@Inject
	private MonitorComponent component;

	@Inject(nullAllowed = false)
	private EventBus eventBus;

	private String fromAddress;
	private Session session;
	private String toAddresses;
	private String username;
	private String password;
	private final EventHandler handler = new EventHandler() {

		@Override
		public void onEvent(String name, String xmlns, Element event) {
			MonitorMailer.this.onEvent(name, xmlns, event);
		}
	};

	public EventBus getEventBus() {
		return eventBus;
	}

	public void setEventBus(EventBus eventBus) {
		this.eventBus = eventBus;
	}

	private String getRequiredProp(Map<String, Object> props, String name) {
		String result;
		try {
			result = (String) props.get(name);
		} catch (Exception e) {
			result = null;
			log.warning("Problem on reading property '" + name + "'");
		}

		if (result == null)
			throw new RuntimeException("Property '" + name + "' is not defined!");

		return result;
	}

	@Override
	public void initialize() {
	}

	protected void onEvent(final String name, final String xmlns, final Element event) {
		if (event.getAttributeStaticStr(REMOTE_EVENT_INDICATOR) != null)
			return;

		String subject = "Tigase Monitor Notification";

		final StringBuilder sb = new StringBuilder();

		sb.append("Tigase Monitor generated event!\n\n");
		sb.append("hostname: ").append(component.getDefHostName()).append('\n');

		if ("SampleTaskEnabled".equals(name)) {
			String t = event.getCData(new String[] { "SampleTaskEnabled", "message" });
			subject += " - " + t;
			sb.append("This is notification from sample monitor task.").append('\n');
			for (Element c : event.getChildren()) {
				sb.append("    ").append(c.getName()).append(": ").append(c.getCData()).append('\n');
			}
		} else {
			sb.append("Event: ").append(name).append('\n');
			for (Element c : event.getChildren()) {
				sb.append("    ").append(c.getName()).append(": ").append(c.getCData()).append('\n');
			}
		}

		sendMail(subject, sb.toString());
	}

	private void sendMail(String messageSubject, String messageText) {
		try {
			log.fine("Sending mail: " + messageText);

			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(fromAddress));

			InternetAddress[] to = InternetAddress.parse(toAddresses);
			message.setRecipients(Message.RecipientType.TO, to);
			message.setSubject(messageSubject);
			message.setText(messageText);

			if (username == null || username.trim().isEmpty()) {
				Transport.send(message, to);
			} else {
				Transport.send(message, to, username, password);
			}
		} catch (Exception e) {
			log.log(Level.WARNING, "Can't send mail", e);
		}
	}

	@Override
	public void setProperties(Map<String, Object> props) {
		log.config("Configuring Monitor Mailer");
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
			sessionProperties.put("mail.smtp.host", getRequiredProp(props, "mailer-smtp-host"));
			sessionProperties.put("mail.smtp.port", getRequiredProp(props, "mailer-smtp-port"));

			this.username = (String) props.get("mailer-smtp-username");
			log.log(Level.CONFIG, "Setting username: {0}", username);
			this.password = (String) props.get("mailer-smtp-password");
			if (password != null && !password.isEmpty()) {
				log.log(Level.CONFIG, "Setting password: {0}", password.replaceAll(".", "*"));
			}

			this.fromAddress = getRequiredProp(props, "mailer-from-address");
			log.log(Level.CONFIG, "Setting fromAddress: {0}", fromAddress);
			this.toAddresses = getRequiredProp(props, "mailer-to-addresses");
			log.log(Level.CONFIG, "Setting toAddresses: {0}", toAddresses);

			this.session = Session.getInstance(sessionProperties);
			log.log(Level.CONFIG, "Setting session: {0}", session);
		} catch (RuntimeException e) {
			log.warning(e.getMessage());
			log.warning("Mailer is not started");
			return;
		}

		log.config("Initializing Monitor Mailer");
		eventBus.addHandler(null, MonitorComponent.EVENTS_XMLNS, handler);
	}

}
