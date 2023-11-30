package tigase.extras.bcstarttls;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.tls.*;
import org.bouncycastle.tls.crypto.TlsCryptoParameters;
import org.bouncycastle.tls.crypto.impl.bc.BcDefaultTlsCredentialedDecryptor;
import org.bouncycastle.tls.crypto.impl.bc.BcDefaultTlsCredentialedSigner;
import org.bouncycastle.tls.crypto.impl.bc.BcTlsCrypto;

import java.io.IOException;
import java.util.Collection;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TigaseTls12Server
		extends DefaultTlsServer {

	private final Collection<X500Name> acceptedIssuers;
	private final Certificate bcCert;
	private final BcTlsCrypto crypto;
	private final AsymmetricKeyParameter privateKey;
	private TlsCredentials m_selectedCredentials;
	private final boolean needClientAuth;
	private final boolean wantClientAuth;
	private Certificate peerCertificate;
	private final HandshakeCompletedListener handshakeCompletedListener;
	private static final Logger log = Logger.getLogger(TigaseTls12Server.class.getName());

	public TigaseTls12Server(BcTlsCrypto crypto, boolean needClientAuth, boolean wantClientAuth,
							 Collection<X500Name> acceptedIssuers, AsymmetricKeyParameter privateKey,
							 Certificate bcCert, HandshakeCompletedListener handshakeCompletedListener) {
		super(crypto);
		this.crypto = crypto;
		this.needClientAuth = needClientAuth;
		this.wantClientAuth = wantClientAuth;
		this.acceptedIssuers = acceptedIssuers;
		this.privateKey = privateKey;
		this.bcCert = bcCert;
		this.handshakeCompletedListener = handshakeCompletedListener;
	}

	public CertificateRequest getCertificateRequest() throws IOException {
		if (!(needClientAuth || wantClientAuth)) {
			return null;
		}

		short[] certificateTypes = new short[]{ClientCertificateType.rsa_sign, ClientCertificateType.dss_sign,
											   ClientCertificateType.ecdsa_sign};

		Vector serverSigAlgs = null;
		if (TlsUtils.isSignatureAlgorithmsExtensionAllowed(context.getServerVersion())) {
			serverSigAlgs = TlsUtils.getDefaultSupportedSignatureAlgorithms(context);
		}

		Vector certificateAuthorities = new Vector();

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
	public void notifyClientCertificate(Certificate clientCertificate) throws IOException {
		this.peerCertificate = clientCertificate;
//				try {
//					X509Certificate[] chain = gen(clientCertificate);
//					if (clientTrustManagers != null) {
//						for (TrustManager ctm : clientTrustManagers) {
//							if (ctm instanceof X509TrustManager) {
//								((X509TrustManager) ctm).checkClientTrusted(chain, "RSA");
//							} else {
//								throw new RuntimeException("Unsupported type of TrustManager " + ctm);
//							}
//						}
//					}
//				} catch (Exception e) {
//					log.log(Level.FINE, "Client certificate is probably untrusted", e);
//					throw new TlsFatalAlert(AlertDescription.certificate_unknown);
//				}
	}

	@Override
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
			handshakeCompletedListener.onHandshakeComplete(peerCertificate, tlsUnique, tlsExporter);
		} catch (Exception e) {
			log.log(Level.WARNING, "Cannot handle handshakeCompleted handler", e);
			throw new TlsFatalAlert(AlertDescription.internal_error);
		}
	}

	@Override
	public void notifySecureRenegotiation(boolean secureRenegotiation) throws IOException {
		// This is required, since the default implementation throws an error if secure reneg is not
		// supported
	}

	@Override
	protected TlsCredentialedDecryptor getRSAEncryptionCredentials() {
		return new BcDefaultTlsCredentialedDecryptor(crypto, bcCert, privateKey);
	}

	@Override
	protected TlsCredentialedSigner getRSASignerCredentials() {
		TlsCryptoParameters crpP = new TlsCryptoParameters(context);
		SignatureAndHashAlgorithm alg = new SignatureAndHashAlgorithm(HashAlgorithm.sha1, SignatureAlgorithm.rsa);

		return new BcDefaultTlsCredentialedSigner(crpP, crypto, privateKey, bcCert, alg);
	}
}
