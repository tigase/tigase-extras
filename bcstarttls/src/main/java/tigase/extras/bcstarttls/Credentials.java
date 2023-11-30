package tigase.extras.bcstarttls;

import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.tls.Certificate;

public class Credentials {

	private final Certificate certificate;
	private final AsymmetricKeyParameter privateKey;

	Credentials(Certificate certificate, AsymmetricKeyParameter privateKey) {
		this.certificate = certificate;
		this.privateKey = privateKey;
	}

	public Certificate getCertificate() {
		return certificate;
	}

	public AsymmetricKeyParameter getPrivateKey() {
		return privateKey;
	}
}
