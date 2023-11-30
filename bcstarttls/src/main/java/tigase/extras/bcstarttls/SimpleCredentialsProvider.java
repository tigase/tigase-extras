package tigase.extras.bcstarttls;

import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.tls.*;
import org.bouncycastle.tls.crypto.TlsCertificate;
import org.bouncycastle.tls.crypto.impl.bc.BcTlsCrypto;
import tigase.io.CertificateContainerIfc;

import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SimpleCredentialsProvider
		implements CredentialsProvider {

	private static final Logger log = Logger.getLogger(SimpleCredentialsProvider.class.getName());

	private final CertificateContainerIfc certificateContainer;
	private final BcTlsCrypto crypto;
	private final String hostname;
	private Credentials credentials;

	private boolean keysLoaded = false;

	public SimpleCredentialsProvider(BcTlsCrypto crypto, CertificateContainerIfc certificateContainer,
									 String hostname) {
		this.crypto = crypto;
		this.certificateContainer = certificateContainer;
		this.hostname = hostname;
	}

	@Override
	public Credentials getCredentials(TlsServerContext context) {
		loadIfRequired(context);
		return this.credentials;
	}

	private Certificate gen12(final java.security.cert.Certificate[] certChain)
			throws CertificateEncodingException, IOException {
		TlsCertificate[] arr = new TlsCertificate[certChain.length];

		for (int i = 0; i < certChain.length; i++) {
			TlsCertificate cc = crypto.createCertificate(certChain[i].getEncoded());
			arr[i] = cc;
		}
		return new org.bouncycastle.tls.Certificate(arr);
	}

	private Certificate gen13(final java.security.cert.Certificate[] certChain)
			throws CertificateEncodingException, IOException {
		CertificateEntry[] arr = new CertificateEntry[certChain.length];

		for (int i = 0; i < certChain.length; i++) {
			TlsCertificate cc = crypto.createCertificate(certChain[i].getEncoded());
			arr[i] = new CertificateEntry(cc, null);
		}
		return new org.bouncycastle.tls.Certificate(CertificateType.X509, TlsUtils.EMPTY_BYTES, arr);
	}

	private void loadIfRequired(final TlsServerContext context) {
		if (keysLoaded) {
			return;
		}
		try {
			tigase.cert.CertificateEntry kk = certificateContainer.getCertificateEntry(hostname);
			var privateKey = PrivateKeyFactory.createKey(kk.getPrivateKey().getEncoded());
			Certificate certificate;
			if (TlsUtils.isTLSv13(context)) {
				certificate = gen13(kk.getCertChain());
			} else {
				certificate = gen12(kk.getCertChain());
			}
			keysLoaded = true;
			this.credentials = new Credentials(certificate, privateKey);
			log.log(Level.FINE, "Certificate for domain loaded.");
		} catch (IOException | CertificateEncodingException e) {
			log.log(Level.WARNING, "Cannot load domain " + hostname + " certificate.", e);
			throw new RuntimeException("Cannot load domain " + hostname + " certificate.", e);
		}
	}

}
