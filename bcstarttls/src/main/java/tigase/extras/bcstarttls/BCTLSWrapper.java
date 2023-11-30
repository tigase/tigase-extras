package tigase.extras.bcstarttls;

import org.bouncycastle.jsse.BCSSLEngine;
import tigase.io.JcaTLSWrapper;
import tigase.io.TLSEventHandler;

import javax.net.ssl.SSLContext;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BCTLSWrapper
		extends JcaTLSWrapper {

	private static final Logger log = Logger.getLogger(BCTLSWrapper.class.getName());

	private byte[] tlsExporterBindingData;
	private byte[] tlsUniqueBindingData;

	public BCTLSWrapper(SSLContext sslc, TLSEventHandler eventHandler, String remote_hostname, int port,
						boolean clientMode, boolean wantClientAuth, boolean needClientAuth, String[] enabledCiphers,
						String[] enabledProtocols) {
		super(sslc, eventHandler, remote_hostname, port, clientMode, wantClientAuth, needClientAuth, enabledCiphers,
			  enabledProtocols);
	}

	@Override
	public byte[] getTlsExporterBindingData() {
		return this.tlsExporterBindingData;
	}

	@Override
	public byte[] getTlsUniqueBindingData() {
		return this.tlsUniqueBindingData;
	}

	@Override
	protected void tlsEngineHandshakeCompleted() {
		super.tlsEngineHandshakeCompleted();
		try {
//			extractChannelBindingData();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Cannot extract Channel Binding Data", e);
		}
	}

	private void extractChannelBindingData() {
		if (this.tlsEngine instanceof BCSSLEngine) {
			this.tlsExporterBindingData = ((BCSSLEngine) this.tlsEngine).getConnection()
					.getChannelBinding("tls-exporter");
			this.tlsUniqueBindingData = ((BCSSLEngine) this.tlsEngine).getConnection().getChannelBinding("tls-unique");
		}
	}
}
