package tigase.extras.bcstarttls;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.crypto.params.ECKeyParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.tls.*;
import org.bouncycastle.tls.crypto.TlsCryptoParameters;
import org.bouncycastle.tls.crypto.impl.bc.BcDefaultTlsCredentialedDecryptor;
import org.bouncycastle.tls.crypto.impl.bc.BcDefaultTlsCredentialedSigner;
import org.bouncycastle.tls.crypto.impl.bc.BcTlsCertificate;
import org.bouncycastle.tls.crypto.impl.bc.BcTlsCrypto;

import java.io.IOException;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DefaultTls13Server
		extends DefaultTlsServer {

	private static final Logger log = Logger.getLogger(DefaultTls13Server.class.getName());
	private final Collection<X500Name> acceptedIssuers;
	private final CredentialsProvider credentialsProvider;
	private final BcTlsCrypto crypto;
	private final HandshakeCompletedListener handshakeCompletedListener;
	private final boolean needClientAuth;
	private final boolean wantClientAuth;
	private List<X500Name> m_clientTrustedIssuers = null;
	private List<Integer> m_peerSigSchemes = null;

	private TlsCredentials m_selectedCredentials = null;
	private Certificate peerCertificate;

	/**
	 * Checks if the given X500Name matches any of the issuers in the provided list.
	 *
	 * @param issuers The list of X500Names representing the issuers.
	 * @param name The X500Name to check for a match against the issuers.
	 *
	 * @return true if a match is found, false otherwise.
	 */
	private static boolean matchesIssuers(List<X500Name> issuers, X500Name name) {
		for (X500Name issuer : issuers) {
			if (name.equals(issuer)) {
				return true;
			}
		}
		return false;
	}

	public DefaultTls13Server(BcTlsCrypto crypto, boolean needClientAuth, boolean wantClientAuth,
							  Collection<X500Name> acceptedIssuers, CredentialsProvider credentialsProvider,
							  HandshakeCompletedListener handshakeCompletedListener) {
		super(crypto);
		this.crypto = crypto;
		this.handshakeCompletedListener = handshakeCompletedListener;
		this.credentialsProvider = credentialsProvider;
		this.needClientAuth = needClientAuth;
		this.wantClientAuth = wantClientAuth;
		this.acceptedIssuers = acceptedIssuers;
	}

	public CertificateRequest getCertificateRequest() throws IOException {
		if (!(needClientAuth || wantClientAuth)) {
			return null;
		}

		short[] certificateTypes = new short[]{ClientCertificateType.rsa_sign, ClientCertificateType.dss_sign,
											   ClientCertificateType.ecdsa_sign};

		//noinspection rawtypes
		Vector serverSigAlgs = null;
		if (TlsUtils.isSignatureAlgorithmsExtensionAllowed(context.getServerVersion())) {
			serverSigAlgs = TlsUtils.getDefaultSupportedSignatureAlgorithms(context);
		}

		Vector<X500Name> certificateAuthorities = new Vector<>();

//      certificateAuthorities.addElement(TlsTestUtils.loadBcCertificateResource("x509-ca-dsa.pem").getSubject());
//      certificateAuthorities.addElement(TlsTestUtils.loadBcCertificateResource("x509-ca-ecdsa.pem").getSubject());
//      certificateAuthorities.addElement(TlsTestUtils.loadBcCertificateResource("x509-ca-rsa.pem").getSubject());

		// All the CA certificates are currently configured with this subject
		//			certificateAuthorities.addElement(new X500Name("CN=BouncyCastle TLS Test CA"));

		if (acceptedIssuers != null) {
			certificateAuthorities.addAll(acceptedIssuers);
		}

		return new CertificateRequest(certificateTypes, serverSigAlgs, certificateAuthorities);
	}

	@Override
	public TlsCredentials getCredentials() throws IOException {
		if (m_selectedCredentials != null) {
			return m_selectedCredentials;
		} else {
			throw new TlsFatalAlert(AlertDescription.internal_error);
		}
	}

	public Certificate getLocalCertificates() {
		return credentialsProvider.getCredentials(context).getCertificate();
	}

	@Override
	public int getSelectedCipherSuite() throws IOException {
		@SuppressWarnings("unchecked") final Vector<SignatureAndHashAlgorithm> clientSigAlgs = (Vector<SignatureAndHashAlgorithm>) context.getSecurityParameters()
				.getClientSigAlgs();
		m_peerSigSchemes = clientSigAlgs.stream().map(SignatureScheme::from).collect(Collectors.toList());
		return super.getSelectedCipherSuite();
	}

	public void notifyClientCertificate(Certificate clientCertificate) throws IOException {
		this.peerCertificate = clientCertificate;
	}

	public void notifyHandshakeComplete() throws IOException {
		super.notifyHandshakeComplete();
		byte[] tlsUnique;
		try {
			tlsUnique = context.exportChannelBinding(ChannelBinding.tls_unique);
		} catch (Exception e) {
			tlsUnique = null;
		}
		byte[] tlsExporter;
		try {
			tlsExporter = context.exportChannelBinding(ChannelBinding.tls_exporter);
		} catch (Exception e) {
			tlsExporter = null;
		}

		try {
			log.log(Level.SEVERE,
					"Handshake complete. tlsUnique=" + (tlsUnique != null) + "; tlsExporter=" + (tlsExporter != null) +
							"; peerCertificate=" + (peerCertificate != null));
			handshakeCompletedListener.onHandshakeComplete(peerCertificate, tlsUnique, tlsExporter);
		} catch (Exception e) {
			log.log(Level.WARNING, "Cannot handle handshakeCompleted handler", e);
			throw new TlsFatalAlert(AlertDescription.internal_error);
		}
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

	protected TlsCredentialedDecryptor getRSAEncryptionCredentials() {
		final var credentials = credentialsProvider.getCredentials(context);
		return new BcDefaultTlsCredentialedDecryptor(crypto, credentials.getCertificate(), credentials.getPrivateKey());
	}

	@Override
	protected TlsCredentialedSigner getRSASignerCredentials() {
		TlsCryptoParameters crpP = new TlsCryptoParameters(context);
		SignatureAndHashAlgorithm alg = new SignatureAndHashAlgorithm(HashAlgorithm.sha1, SignatureAlgorithm.rsa);
		final var credentials = credentialsProvider.getCredentials(context);
		return new BcDefaultTlsCredentialedSigner(crpP, crypto, credentials.getPrivateKey(),
												  credentials.getCertificate(), alg);
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

	private TlsCredentials createCredentialedSigner13(final SignatureAndHashAlgorithm signatureScheme) {
		if (crypto == null) {
			throw new RuntimeException("Crypto in not BcTlsCrypto");
		}
		log.finest(() -> "selected peer sig schema " + signatureScheme);
		final var credentials = credentialsProvider.getCredentials(context);
		return new BcDefaultTlsCredentialedSigner(new TlsCryptoParameters(context), crypto, credentials.getPrivateKey(),
												  credentials.getCertificate(), signatureScheme);
	}

	private boolean isSuitableCredentials(final SignatureAndHashAlgorithm algorithm) throws IOException {
		final var credentials = credentialsProvider.getCredentials(context);
		var certificate = credentials.getCertificate();
		if (certificate.isEmpty()) {
			return false;
		}

		var algId = SignatureScheme.from(algorithm);

		// check if private key matches signature schema
		// TODO: maybe it is not needed and certificate key check done below would be enough? that would simplify things a lot
		if ((!(SignatureScheme.isRSAPSS(algId) && credentials.getPrivateKey() instanceof RSAKeyParameters)) &&
				(!(SignatureScheme.isECDSA(algId) && credentials.getPrivateKey() instanceof ECKeyParameters))) {
			return false;
		}

		// use only algorithms for which matches key used by certificate
		if (!certificate.getCertificateAt(0).supportsSignatureAlgorithm(SignatureScheme.getSignatureAlgorithm(algId))) {
			return false;
		}

		if (m_clientTrustedIssuers == null || m_clientTrustedIssuers.isEmpty()) {
			return true;
		}
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
		return switch (keyExchangeAlgorithm) {
			case KeyExchangeAlgorithm.NULL -> selectServerCredentials13();
			case KeyExchangeAlgorithm.DHE_RSA, KeyExchangeAlgorithm.ECDHE_RSA -> getRSASignerCredentials();
			case KeyExchangeAlgorithm.DHE_DSS -> getDSASignerCredentials();
			case KeyExchangeAlgorithm.ECDHE_ECDSA -> getECDSASignerCredentials();
			case KeyExchangeAlgorithm.RSA -> getRSAEncryptionCredentials();
			default -> null;
		};
	}

	private TlsCredentials selectServerCredentials13() throws IOException {

		if (log.isLoggable(Level.FINEST)) {
			log.finest(() -> "selecting peer sig schema from " +
					m_peerSigSchemes.stream().map(SignatureScheme::getSignatureAndHashAlgorithm).toList());
		}

		SignatureAndHashAlgorithm best = null;
		for (int peerSigScheme : m_peerSigSchemes) {
			SignatureAndHashAlgorithm current = SignatureScheme.getSignatureAndHashAlgorithm(peerSigScheme);
			if (current == null) {
				continue;
			}
			if (!isSuitableCredentials(current)) {
				continue;
			}
			// TODO: we are using the last one as in my tests, last one had best hash algorithm, but that may be wrong in theory we could use first found
			best = current;
		}
		if (best != null) {
			return createCredentialedSigner13(best);
		}
		return null;
	}

}
