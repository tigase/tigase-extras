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
