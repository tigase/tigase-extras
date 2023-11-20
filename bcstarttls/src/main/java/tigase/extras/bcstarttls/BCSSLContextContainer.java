/*
 * Tigase Server Extras Bouncycastle for StartTLS - Extra modules to Tigase Server
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
package tigase.extras.bcstarttls;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import tigase.io.*;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.nio.ByteOrder;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.util.logging.Logger;

public class BCSSLContextContainer
		extends SSLContextContainer {

	private static final Logger log = Logger.getLogger(BCSSLContextContainer.class.getName());

	@Override
	public IOInterface createIoInterface(String protocol, String local_hostname, String remote_hostname, int port,
										 boolean clientMode, boolean wantClientAuth, boolean needClientAuth,
										 ByteOrder byteOrder, TrustManager[] x509TrustManagers,
										 TLSEventHandler eventHandler, IOInterface socketIO,
										 CertificateContainerIfc certificateContainer) throws IOException {
		return new BcTLSIO(certificateContainer, eventHandler, socketIO, local_hostname, byteOrder, wantClientAuth,
						   needClientAuth, getEnabledCiphers(), getEnabledProtocols(), x509TrustManagers);
	}

	@Override
	public void initialize() {
		log.config("Installing BouncyCastle provider");
		Security.addProvider(new BouncyCastleProvider());
		Security.addProvider(new org.bouncycastle.jsse.provider.BouncyCastleJsseProvider());
		super.initialize();
	}

	@Override
	protected SSLContext createSSLContext(String protocol) throws NoSuchAlgorithmException, NoSuchProviderException {
		return SSLContext.getInstance(protocol, BouncyCastleJsseProvider.PROVIDER_NAME);
	}

}
