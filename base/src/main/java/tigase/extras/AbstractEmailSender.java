/*
 * AbstractEmailSender.java
 *
 * Tigase Jabber/XMPP Server - Extras
 * Copyright (C) 2004-2017 "Tigase, Inc." <office@tigase.com>
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

package tigase.extras;

import groovy.lang.Writable;
import groovy.text.SimpleTemplateEngine;
import groovy.text.Template;
import tigase.extras.mailer.Mailer;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;

import java.io.*;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AbstractEmailSender {

	private static final Logger log = Logger.getLogger(AbstractEmailSender.class.getCanonicalName());

	@Inject
	private Mailer mailer;
	private SimpleTemplateEngine ste = new SimpleTemplateEngine();
	@ConfigField(desc = "Notification email subject")
	private String subject;
	@ConfigField(desc = "Email template file")
	private String templateFile;

	public AbstractEmailSender(String subject, String templateFile) {
		this.subject = subject;
		this.templateFile = templateFile;
	}

	public void sendMail(String email, Map<String, Object> bindings) throws Exception {
		Writable w = null;
		try {
			Template template = ste.createTemplate(load(templateFile));
			w = template.make(bindings);
			mailer.sendMail(email, subject, w.toString());
		} catch (Exception ex) {
			log.log(Level.FINE, "Failed to send email to " + email + ", message: " + w, ex);
			throw ex;
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

}
