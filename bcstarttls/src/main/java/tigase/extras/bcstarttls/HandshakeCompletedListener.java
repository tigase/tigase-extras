package tigase.extras.bcstarttls;

import org.bouncycastle.tls.Certificate;

@FunctionalInterface
public interface HandshakeCompletedListener {

	void onHandshakeComplete(Certificate clientCertificate, byte[] tlsUnique, byte[] tlsExporter);

}
