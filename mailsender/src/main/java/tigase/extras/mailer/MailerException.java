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

/**
 * Created by bmalkow on 21.04.2017.
 */
public class MailerException
		extends RuntimeException {

	public MailerException() {
	}

	public MailerException(String message) {
		super(message);
	}

	public MailerException(String message, Throwable cause) {
		super(message, cause);
	}

	public MailerException(Throwable cause) {
		super(cause);
	}

	public MailerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
