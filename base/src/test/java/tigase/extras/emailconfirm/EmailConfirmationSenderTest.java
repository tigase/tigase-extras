/*
 * Tigase Server Extras Base - Extra modules to Tigase Server
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
package tigase.extras.emailconfirm;

import org.junit.Before;
import org.junit.Test;
import tigase.xmpp.XMPPProcessorException;

import java.util.Map;

public class EmailConfirmationSenderTest {

	private EmailConfirmationSender emailConfirmationSender;
	@Before
	public void setUp() {
		emailConfirmationSender = new EmailConfirmationSender();
	}

	@Test(expected = XMPPProcessorException.class)
	public void checkRequiredParametersWithoutAt() throws XMPPProcessorException {
		emailConfirmationSender.checkRequiredParameters(null, Map.of("email", "mark"));
	}

	@Test(expected = XMPPProcessorException.class)
	public void checkRequiredParametersEmpty() throws XMPPProcessorException {
		emailConfirmationSender.checkRequiredParameters(null, Map.of("email", ""));
	}

	@Test(expected = XMPPProcessorException.class)
	public void checkRequiredParametersDomain() throws XMPPProcessorException {
		emailConfirmationSender.checkRequiredParameters(null, Map.of("email", "example.com"));
	}

	@Test(expected = XMPPProcessorException.class)
	public void checkRequiredParametersWithSpace1() throws XMPPProcessorException {
		emailConfirmationSender.checkRequiredParameters(null, Map.of("email", "ffasdf    ssdfsdf@com"));
	}

	@Test(expected = XMPPProcessorException.class)
	public void checkRequiredParametersWithSpace2() throws XMPPProcessorException {
		emailConfirmationSender.checkRequiredParameters(null, Map.of("email", "ffasdf    ssdfsdf@com     "));
	}

	@Test(expected = XMPPProcessorException.class)
	public void checkRequiredParametersWithSpace3() throws XMPPProcessorException {
		emailConfirmationSender.checkRequiredParameters(null, Map.of("email", "    ffasdf    ssdfsdf@com"));
	}
	@Test
	public void checkRequiredParametersCorrectAddress() throws XMPPProcessorException {
		emailConfirmationSender.checkRequiredParameters(null, Map.of("email", "mail@domain.com"));
	}
}