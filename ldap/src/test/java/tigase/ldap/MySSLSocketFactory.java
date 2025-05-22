/*
 * Tigase Server Extras LDAP Server - Extra modules to Tigase Server
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

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementation of SocketFactory providing socket with SSL/TLS support ignoring SSL certificate verification.
 */
public class MySSLSocketFactory extends SocketFactory {
	private static final AtomicReference<MySSLSocketFactory> defaultFactory = new AtomicReference<>();

	private SSLSocketFactory sf;

	public MySSLSocketFactory() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
		SSLContext ctx = SSLContext.getInstance("TLS");
		ctx.init(null, new TrustManager[]{
				new DummyTrustManager()
		}, new SecureRandom());
		sf = ctx.getSocketFactory();
	}

	public static SocketFactory getDefault() {
		final MySSLSocketFactory value = defaultFactory.get();
		if (value == null) {
			try {
				defaultFactory.compareAndSet(null, new MySSLSocketFactory());
				return defaultFactory.get();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return value;
	}

	@Override
	public Socket createSocket(final String s, final int i) throws IOException {
		return sf.createSocket(s, i);
	}

	@Override
	public Socket createSocket(final String s, final int i, final InetAddress inetAddress, final int i1) throws IOException {
		return sf.createSocket(s, i, inetAddress, i1);
	}

	@Override
	public Socket createSocket(final InetAddress inetAddress, final int i) throws IOException {
		return sf.createSocket(inetAddress, i);
	}

	@Override
	public Socket createSocket(final InetAddress inetAddress, final int i, final InetAddress inetAddress1, final int i1) throws IOException {
		return sf.createSocket(inetAddress, i, inetAddress1, i1);
	}
}
