/*
 * Tigase Server Extras MongoDB - Extra modules to Tigase Server
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
package tigase.ldap;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedTrustManager;
import java.net.Socket;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class DummyTrustManager
		extends X509ExtendedTrustManager {
	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket)
							throws CertificateException {

	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket)
							throws CertificateException {

	}

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
							throws CertificateException {

	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
							throws CertificateException {

	}

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType)
							throws CertificateException {

	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType)
							throws CertificateException {

	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		return new X509Certificate[0];
	}
}
