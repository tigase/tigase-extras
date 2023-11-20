package tigase.extras.bcstarttls;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.tls.*;
import org.bouncycastle.tls.crypto.TlsCrypto;
import org.bouncycastle.tls.crypto.TlsCryptoParameters;
import org.bouncycastle.tls.crypto.impl.bc.BcDefaultTlsCredentialedSigner;
import org.bouncycastle.tls.crypto.impl.bc.BcTlsCertificate;
import org.bouncycastle.tls.crypto.impl.bc.BcTlsCrypto;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DefaultTls13Server
		extends AbstractTlsServer {

	private static final Logger log = Logger.getLogger(DefaultTls13Server.class.getName());

	private final static int[] DefaultCipherSuites = new int[]{CipherSuite.TLS_AES_256_GCM_SHA384,
															   CipherSuite.TLS_AES_128_GCM_SHA256,
															   CipherSuite.TLS_CHACHA20_POLY1305_SHA256,};
	private final TlsCrypto crypto;
	private final Map<Integer, Credentials> m_credentials = new HashMap<>();
	private List<X500Name> m_clientTrustedIssuers = null;
	private List<Integer> m_peerSigSchemes = null;

	private TlsCredentials m_selectedCredentials = null;

	private static boolean matchesIssuers(List<X500Name> issuers, X500Name name) {
		for (X500Name issuer : issuers) {
			if (name.equals(issuer)) {
				return true;
			}
		}
		return false;
	}

	public DefaultTls13Server(TlsCrypto crypto, AsymmetricKeyParameter privateKey, Certificate bcCert) {
		super(crypto);
		this.crypto = crypto;

		// TODO Fill in m_credentials according to which SignatureScheme values we support e.g.:
//		m_credentials.put(SignatureScheme.rsa_pkcs1_sha384, new Credentials(bcCert, privateKey));
		m_credentials.put(SignatureScheme.rsa_pkcs1_sha256, new Credentials(bcCert, privateKey));
	}

	@Override
	public TlsCredentials getCredentials() throws IOException {
		if (m_selectedCredentials != null) {
			return m_selectedCredentials;
		} else {
			throw new TlsFatalAlert(AlertDescription.internal_error);
		}
	}

	@Override
	public int getSelectedCipherSuite() throws IOException {
		@SuppressWarnings("unchecked") final Vector<SignatureAndHashAlgorithm> clientSigAlgs = (Vector<SignatureAndHashAlgorithm>) context.getSecurityParameters()
				.getClientSigAlgs();
		m_peerSigSchemes = clientSigAlgs.stream().map(SignatureScheme::from).collect(Collectors.toList());
		return super.getSelectedCipherSuite();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void processClientExtensions(Hashtable clientExtensions) throws IOException {
		super.processClientExtensions(clientExtensions);
		Vector<X500Name> v = ((Vector<X500Name>) TlsExtensionsUtils.getCertificateAuthoritiesExtension(
				clientExtensions));
		if (v == null) {
			return;
		}
		m_clientTrustedIssuers = v.stream().toList();
	}

	@Override
	protected int[] getSupportedCipherSuites() {
		return TlsUtils.getSupportedCipherSuites(crypto, DefaultCipherSuites);
	}

	@Override
	protected boolean selectCipherSuite(int cipherSuite) throws IOException {
		TlsCredentials cipherSuiteCredentials = null;
		final int keyExchangeAlgorithm = TlsUtils.getKeyExchangeAlgorithm(cipherSuite);
		if (!KeyExchangeAlgorithm.isAnonymous(keyExchangeAlgorithm)) {
			cipherSuiteCredentials = selectCredentials(keyExchangeAlgorithm);
			if (null == cipherSuiteCredentials) {
				return false;
			}
		}
		boolean result = super.selectCipherSuite(cipherSuite);
		if (result) {
			m_selectedCredentials = cipherSuiteCredentials;
		}
		return result;
	}

	private TlsCredentials createCredentialedSigner13(final int signatureScheme, final Credentials credentials) {
		if (!(crypto instanceof BcTlsCrypto)) {
			throw new RuntimeException("Crypto in not BcTlsCrypto");
		}
		return new BcDefaultTlsCredentialedSigner(new TlsCryptoParameters(context), (BcTlsCrypto) crypto,
												  credentials.privateKey, credentials.certificate,
												  SignatureScheme.getSignatureAndHashAlgorithm(signatureScheme));
	}

	private boolean isSuitableCredentials(final Credentials credentials) throws IOException {
		var certificate = credentials.certificate;
		if (certificate.isEmpty()) {
			return false;
		}
		if (m_clientTrustedIssuers == null || m_clientTrustedIssuers.isEmpty()) {
			return true;
		}
		var crypto = (BcTlsCrypto) this.crypto;
		var chain = certificate.getCertificateList();
		int pos = chain.length;
		while (--pos >= 0) {
			var issuer = BcTlsCertificate.convert(crypto, chain[pos]).getCertificate().getIssuer();
			if (matchesIssuers(m_clientTrustedIssuers, issuer)) {
				return true;
			}
		}
		var eeCert = BcTlsCertificate.convert(crypto, chain[0]);
		var basicConstraints = BasicConstraints.getInstance(eeCert);
		return basicConstraints != null && basicConstraints.getPathLenConstraint().longValue() > 0 &&
				matchesIssuers(m_clientTrustedIssuers, eeCert.getCertificate().getSubject());
	}

	private TlsCredentials selectCredentials(int keyExchangeAlgorithm) throws IOException {
		switch (keyExchangeAlgorithm) {
			case KeyExchangeAlgorithm.NULL -> {
				return selectServerCredentials13();
			}
			default -> {
				return null;
			}
		}
	}

	private TlsCredentials selectServerCredentials13() throws IOException {
		for (int peerSigScheme : m_peerSigSchemes) {
			if (!m_credentials.containsKey(peerSigScheme)) {
				continue;
			}
			final Credentials candidateCredentials = m_credentials.get(peerSigScheme);
			if (!isSuitableCredentials(candidateCredentials)) {
				continue;
			}
			return createCredentialedSigner13(peerSigScheme, candidateCredentials);
		}
		return null;
	}

	private static class Credentials {

		final Certificate certificate;
		final AsymmetricKeyParameter privateKey;

		public Credentials(Certificate certificate, AsymmetricKeyParameter privateKey) {
			this.certificate = certificate;
			this.privateKey = privateKey;
		}
	}
}
