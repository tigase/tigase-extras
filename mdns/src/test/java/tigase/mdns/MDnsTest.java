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
import javax.jmdns.NetworkTopologyDiscovery;
import javax.jmdns.ServiceInfo;
import javax.jmdns.impl.HostInfo;
import javax.jmdns.impl.JmDNSImpl;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayDeque;

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

	@Ignore
	@Test
	public void test2() throws IOException, InterruptedException {
		ArrayDeque<JmDNS> mdnss = new ArrayDeque<>();
		InetAddress[] addresses = NetworkTopologyDiscovery.Factory.getInstance().getInetAddresses();
		for (InetAddress addr : addresses) {
			if (addr.isLinkLocalAddress()) {
				continue;
			}
			System.out.println("creating jmdns for " + addr);
			JmDNSImpl jmDNS = (JmDNSImpl) JmDNS.create(addr, "tigase-iot-hub.local");
			HostInfo info = jmDNS.getLocalHost();
			System.out.println("got host info: addr=" + info.getInetAddress() + ", ifc=" + info.getInterface());
			mdnss.add(jmDNS);
		}

		System.out.println("started...");
		Thread.sleep(10000);
		System.out.println("results for tigase-iot-hub.local: [");
		addresses = InetAddress.getAllByName("tigase-iot-hub.local");
		for (InetAddress address : addresses) {
			System.out.println("found: " + address);
		}
		System.out.println("]");
		Thread.sleep(1000*1000);
		System.out.println("stopping...");

		JmDNS mdns = null;
		while ((mdns = mdnss.poll()) != null) {
			System.out.println("stopping jmdns for " + mdns.getInetAddress());
			mdns.close();
		}
	}
}
