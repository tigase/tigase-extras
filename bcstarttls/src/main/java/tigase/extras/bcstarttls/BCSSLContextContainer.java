/**
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

import tigase.io.CertificateContainerIfc;
import tigase.io.IOInterface;
import tigase.io.SSLContextContainer;
import tigase.io.TLSEventHandler;

import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.nio.ByteOrder;

public class BCSSLContextContainer
		extends SSLContextContainer {

	@Override
	public IOInterface createIoInterface(String protocol, String tls_hostname, int port, boolean clientMode,
										 boolean wantClientAuth, boolean needClientAuth, ByteOrder byteOrder,
										 TrustManager[] x509TrustManagers, TLSEventHandler eventHandler,
										 IOInterface socketIO, CertificateContainerIfc certificateContainer)
			throws IOException {
		return new BcTLSIO(certificateContainer, eventHandler, socketIO, tls_hostname, byteOrder, wantClientAuth,
						   needClientAuth, getEnabledCiphers(), getEnabledProtocols(), x509TrustManagers);
	}
}
