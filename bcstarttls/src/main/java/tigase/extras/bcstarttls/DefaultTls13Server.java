package tigase.extras.bcstarttls;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.ECKeyParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.tls.*;
import org.bouncycastle.tls.crypto.TlsCrypto;
import org.bouncycastle.tls.crypto.TlsCryptoParameters;
import org.bouncycastle.tls.crypto.impl.bc.BcDefaultTlsCredentialedSigner;
import org.bouncycastle.tls.crypto.impl.bc.BcTlsCertificate;
import org.bouncycastle.tls.crypto.impl.bc.BcTlsCrypto;

import java.io.IOException;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DefaultTls13Server
		extends AbstractTlsServer {

	private static final Logger log = Logger.getLogger(DefaultTls13Server.class.getName());

	private final static int[] DefaultCipherSuites = new int[]{CipherSuite.TLS_AES_256_GCM_SHA384,
															   CipherSuite.TLS_AES_128_GCM_SHA256,
															   CipherSuite.TLS_CHACHA20_POLY1305_SHA256,};
	private final TlsCrypto crypto;
	private final Credentials credentials;
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
		credentials = new Credentials(bcCert, privateKey);
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

	private TlsCredentials createCredentialedSigner13(final SignatureAndHashAlgorithm signatureScheme, final Credentials credentials) {
		if (!(crypto instanceof BcTlsCrypto)) {
			throw new RuntimeException("Crypto in not BcTlsCrypto");
		}
		log.finest(() -> "selected peer sig schema " + signatureScheme);
		return new BcDefaultTlsCredentialedSigner(new TlsCryptoParameters(context), (BcTlsCrypto) crypto,
												  credentials.privateKey, credentials.certificate,
												  signatureScheme);
	}
	
	private boolean isSuitableCredentials(final Credentials credentials, final SignatureAndHashAlgorithm algorithm) throws IOException {
		var certificate = credentials.certificate;
		if (certificate.isEmpty()) {
			return false;
		}

		var algId = SignatureScheme.from(algorithm);

		// check if private key matches signature schema
		// TODO: maybe it is not needed and certificate key check done below would be enough? that would simplify things a lot
		if ((!(SignatureScheme.isRSAPSS(algId) && credentials.privateKey instanceof RSAKeyParameters)) &&
				(!(SignatureScheme.isECDSA(algId) && credentials.privateKey instanceof ECKeyParameters))) {
			return false;
		}

		// use only algorithms for which matches key used by certificate
		if (!certificate.getCertificateAt(0).supportsSignatureAlgorithm(SignatureScheme.getSignatureAlgorithm(algId))) {
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
		if (log.isLoggable(Level.FINEST)) {
			log.finest(() -> "selecting peer sig schema from " + m_peerSigSchemes.stream().map(SignatureScheme::getSignatureAndHashAlgorithm).toList());
		}

		SignatureAndHashAlgorithm best = null;
		for (int peerSigScheme : m_peerSigSchemes) {
			SignatureAndHashAlgorithm current = SignatureScheme.getSignatureAndHashAlgorithm(peerSigScheme);
			if (current == null) {
				continue;
			}
			if (!isSuitableCredentials(credentials, current)) {
				continue;
			}
			// TODO: we are using the last one as in my tests, last one had best hash algorithm, but that may be wrong in theory we could use first found
			best = current;
		}
		if (best != null) {
			return createCredentialedSigner13(best, credentials);
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
