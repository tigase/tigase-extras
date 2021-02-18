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

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import tigase.component.DSLBeanConfigurator;
import tigase.component.DSLBeanConfiguratorWithBackwardCompatibility;
import tigase.conf.LoggingBean;
import tigase.db.MsgRepositoryIfc;
import tigase.db.xml.XMLRepository;
import tigase.kernel.AbstractKernelTestCase;
import tigase.kernel.DefaultTypesConverter;
import tigase.kernel.core.Kernel;
import tigase.server.amp.db.MsgRepository;
import tigase.vhosts.VHostItemDefaults;
import tigase.vhosts.VHostItemExtensionManager;
import tigase.vhosts.VHostJDBCRepositoryTest;
import tigase.xmpp.impl.MessageAmp;
import tigase.xmpp.jid.JID;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;
@Ignore
public class MailerTest extends AbstractKernelTestCase {

	Mailer mailer;
	private Kernel kernel;

	@Override
	protected void registerBeans(Kernel kernel) {
		this.kernel = kernel;
	}


	@Before
	public void setUp() throws Exception {
		kernel.setName("mailer");
		kernel.setForceAllowNull(true);
		super.registerBeans(kernel);
		kernel.registerBean(Mailer.class).exportable().exec();
		kernel.registerBean(DefaultTypesConverter.class).exec();
		kernel.registerBean(DSLBeanConfiguratorWithBackwardCompatibility.class).exportable().exec();
		final DSLBeanConfiguratorWithBackwardCompatibility config = kernel.getInstance(
				DSLBeanConfiguratorWithBackwardCompatibility.class);

		Map<String, Object> props = new HashMap<>();
		props.put("name", "mailer");
		props.put("mailer-smtp-username", "");
		props.put("mailer-smtp-password", "");
		props.put("mailer-from-address", "");
		props.put("mailer-smtp-host", "");
//		props.put("mailer-smtp-port", "587");

		config.setProperties(props);

		mailer = getInstance(Mailer.class);
	}

	@Test
	public void sendMail() {
		if (mailer != null) {
			final UUID uuid = UUID.randomUUID();
			mailer.sendMail("wojtek@tigase.net", "From UnitTest: " + uuid, uuid.toString());
		}
	}
}