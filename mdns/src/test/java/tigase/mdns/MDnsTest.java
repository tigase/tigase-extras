/*
 * MDnsTest.java
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

package tigase.mdns;

import org.junit.Ignore;
import org.junit.Test;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.IOException;

public class MDnsTest {

	@Ignore
	@Test
	public void test() throws IOException, InterruptedException {
		try (JmDNS jmDNS = JmDNS.create("tigase-iot")) {
			ServiceInfo info = ServiceInfo.create("_xmpp-client._tcp.local.", "tigase-iot", 5222, "Tigase IOT HUB");

			System.out.println("info: " + info.toString());
			jmDNS.registerService(info);

			Thread.sleep(1000 * 1000);
		}
	}
}
