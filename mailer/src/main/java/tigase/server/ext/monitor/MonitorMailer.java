/*
 * Tigase Server Extras Mailer - Sending mail
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
package tigase.server.ext.monitor;

import tigase.eventbus.EventBus;
import tigase.eventbus.EventListener;
import tigase.extras.mailer.Mailer;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;
import tigase.monitor.MonitorComponent;
import tigase.monitor.MonitorExtension;
import tigase.util.StringUtilities;
import tigase.xml.Element;
import tigase.xml.XMLUtils;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Bean(name = "monitor-mailer", active = true, exportable = true)
public class MonitorMailer
		implements MonitorExtension, Initializable, UnregisterAware {

	private final static String REMOTE_EVENT_INDICATOR = "remote";
	private final Logger log = Logger.getLogger(this.getClass().getName());
	@Inject
	private MonitorComponent component;

	@Inject
	private EventBus eventBus;

	@ConfigField(desc = "Email notification sender", alias = "from-address")
	private String fromAddress = null;
	@Inject
	private Mailer mailSender;
	@ConfigField(desc = "Email notification recipients", alias = "to-addresses")
	private String toAddresses;
	private final EventListener<Element> handler = new EventListener<Element>() {
		@Override
		public void onEvent(Element event) {
			String eventFullName = ((Element) event).getName();
			int i = eventFullName.lastIndexOf(".");
			final String packageName = i >= 0 ? eventFullName.substring(0, i) : "";
			final String eventName = eventFullName.substring(i + 1);

			MonitorMailer.this.onEvent(eventName, packageName, event);
		}
	};

	public EventBus getEventBus() {
		return eventBus;
	}

	public void setEventBus(EventBus eventBus) {
		this.eventBus = eventBus;
	}

	@Override
	public void beforeUnregister() {
		eventBus.removeListener(handler);
	}

	@Override
	public void initialize() {
		log.config("Initializing Monitor Mailer");
		eventBus.addListener("tigase.monitor.tasks", null, handler);
	}

	@Override
	public void setProperties(Map<String, Object> props) {
		log.config("Configuring Monitor Mailer");
		try {
			this.fromAddress = getRequiredProp(props, "mailer-from-address");
			log.log(Level.CONFIG, "Setting fromAddress: {0}", fromAddress);
			this.toAddresses = getRequiredProp(props, "mailer-to-addresses");
			log.log(Level.CONFIG, "Setting toAddresses: {0}", toAddresses);

		} catch (RuntimeException e) {
			log.warning(e.getMessage());
			log.warning("Mailer is not started");
			return;
		}

	}

	protected void onEvent(final String name, final String xmlns, final Element event) {
		if (event.getAttributeStaticStr(REMOTE_EVENT_INDICATOR) != null) {
			return;
		}

		String subject = "Tigase Monitor Notification: " + name;

		final StringBuilder sb = new StringBuilder();

		sb.append("Tigase Monitor generated event!\n\n");
		sb.append("hostname: ").append(component.getDefHostName()).append('\n');

		if ("SampleTaskEnabled".equals(name)) {
			String t = event.getCData(new String[]{"tigase.monitor.tasks.SampleTaskEnabled", "message"});
			subject += " - " + t;
			sb.append("This is notification from sample monitor task.").append('\n');
			for (Element c : event.getChildren()) {
				sb.append("    ").append(c.getName()).append(": ").append(c.getCData()).append('\n');
			}
		} else {
			sb.append("Event: ").append(name).append('\n');
			for (Element c : event.getChildren()) {
				sb.append("    ").append(c.getName()).append(": ").append(XMLUtils.unescape(c.getCData())).append('\n');
			}
		}

		sendMail(subject, sb.toString());
	}

	private String getRequiredProp(Map<String, Object> props, String name) {
		String result;
		try {
			result = (String) props.get(name);
		} catch (Exception e) {
			result = null;
			log.warning("Problem on reading property '" + name + "'");
		}

		if (result == null) {
			throw new RuntimeException("Property '" + name + "' is not defined!");
		}

		return result;
	}

	private void sendMail(String messageSubject, String messageText) {
		mailSender.sendMail(fromAddress, toAddresses, messageSubject, messageText);
	}

}
