package tigase.extras.bcstarttls;

import org.bouncycastle.jsse.BCSSLEngine;
import org.bouncycastle.tls.TlsUtils;
import tigase.io.JcaTLSWrapper;
import tigase.io.TLSEventHandler;

import javax.net.ssl.SSLContext;

public class BCTLSWrapper
		extends JcaTLSWrapper {


	public BCTLSWrapper(SSLContext sslc, TLSEventHandler eventHandler, String remote_hostname, int port,
						boolean clientMode, boolean wantClientAuth, boolean needClientAuth, String[] enabledCiphers,
						String[] enabledProtocols) {
		super(sslc, eventHandler, remote_hostname, port, clientMode, wantClientAuth, needClientAuth, enabledCiphers,
			  enabledProtocols);
	}



	@Override
	protected void tlsEngineHandshakeCompleted() {
		super.tlsEngineHandshakeCompleted();
	}


}
