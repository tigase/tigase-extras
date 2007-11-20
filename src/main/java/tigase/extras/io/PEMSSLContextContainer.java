package tigase.extras.io;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.Key;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PasswordFinder;

public class PEMSSLContextContainer {

	private static class DummyTrustManager implements X509TrustManager {

		public void checkClientTrusted(final X509Certificate[] x509CertificateArray, final String string)
				throws CertificateException {}

		public void checkServerTrusted(final X509Certificate[] x509CertificateArray, final String string)
				throws CertificateException {}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}

	}

	private static final Logger log = Logger.getLogger(PEMSSLContextContainer.class.getName());

	public KeyStore loadFromPEMFile(String fileName, String alias, final String privateKeyPassphrase)
			throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
		PEMReader reader = null;
		PasswordFinder x = new PasswordFinder() {

			@Override
			public char[] getPassword() {
				if (privateKeyPassphrase != null) {
					return privateKeyPassphrase.toCharArray();
				} else {
					return null;
				}
			}
		};
		try {
			log.info("Reading private key & certificate chain from file " + fileName);
			List<Certificate> certs = new ArrayList<Certificate>();
			Key key = null;
			reader = new PEMReader(new FileReader(fileName), x);
			Object pemObject = null;
			while ((pemObject = reader.readObject()) != null) {
				if (pemObject instanceof Certificate) {
					certs.add((Certificate) pemObject);
				} else if (pemObject instanceof KeyPair) {
					key = ((KeyPair) pemObject).getPrivate();
				} else if (pemObject instanceof Key) {
					key = (Key) pemObject;
				}
			}
			KeyStore keyStore = KeyStore.getInstance(KEY_STORE_ALGORITHM);
			keyStore.load(null, internalKeystorePassword.toCharArray());

			keyStore.setKeyEntry(alias, key, internalKeystorePassword.toCharArray(),
					certs.toArray(new Certificate[] {}));
			return keyStore;
		} finally {
			if (reader != null) {
				reader.close();
			}
		}

	}

	public static void main(String[] args) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
		PEMSSLContextContainer x = new PEMSSLContextContainer();
		x.init();

		x.getSSLContext("ssl", "malkowscy.net");

	}

	/**
	 * Path to directory with CA certificates.<br/>In most Linux systems it may
	 * be: <code>/etc/ssl/certs</code>.
	 */
	private String caCertsPath = "";

	/**
	 * Path to directory with private keys and certificate for domain.<br/>
	 * File name MUST have name in format <code><i>domainname</i>.cer</code>,
	 * for example:
	 * <ul>
	 * <li><code>tigase.org.cer</code>
	 * <li>
	 * <li><code>malkowscy.net.cer</code></li>
	 * </ul>
	 */
	private String domainKeysPath = "";

	/**
	 * When private keys are encrypted, they MUST be encrypted with one
	 * passphrase!
	 */
	private String privateKeyPassphrase = "";

	private Map<String, SSLContext> sslContexts = new HashMap<String, SSLContext>();

	/**
	 * Allow (or not) to trust all other server. If <code>false</code>, then
	 * CA certificate will be used to check peer certificates.
	 */
	private boolean trustEveryone = false;

	private TrustManagerFactory trustManagerFactory;

	public SSLContext getSSLContext(final String protocol, String hostname) throws NoSuchAlgorithmException,
			KeyManagementException, UnrecoverableKeyException, KeyStoreException, CertificateException, IOException {
		String map_key = hostname + ":" + protocol;
		SSLContext sslContext = sslContexts.get(map_key);
		if (sslContext == null) {
			sslContext = SSLContext.getInstance(protocol);
			TrustManager[] trustManagers;
			if (trustEveryone) {
				trustManagers = new TrustManager[] { new DummyTrustManager() };
			} else {
				trustManagers = trustManagerFactory.getTrustManagers();
			}

			log.fine("Creating SSLConext for " + protocol + ":" + hostname + " with "
					+ (trustEveryone ? "trust everyone option." : "CA certs based trust model."));

			String path = new File(domainKeysPath).getAbsoluteFile() + "/" + hostname + ".cer";
			KeyStore keyStore = loadFromPEMFile(path, hostname, privateKeyPassphrase);
			KeyManagerFactory kmf = KeyManagerFactory.getInstance(KEY_MANAGER_ALGORITHM);
			kmf.init(keyStore, privateKeyPassphrase.toCharArray());

			sslContext.init(kmf.getKeyManagers(), trustManagers, new SecureRandom());
			sslContexts.put(map_key, sslContext);
		}

		return sslContext;
	}

	public void init() throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
		File root = new File(caCertsPath);
		File[] files = root.listFiles();
		KeyStore trustKeyStore = KeyStore.getInstance(KEY_STORE_ALGORITHM);
		trustKeyStore.load(null, internalKeystorePassword.toCharArray());
		log.info("Loading trusted CA certificates from " + root.getAbsolutePath() + "...");
		if (files != null) {
			for (File file : files) {
				try {
					List<Object> objs = readObjectsFromFile(file, null);
					for (Object object : objs) {
						if (object instanceof X509Certificate) {
							X509Certificate crt = (X509Certificate) object;
							String alias = crt.getSubjectDN().getName();
							trustKeyStore.setCertificateEntry(alias, crt);
							log.finest("Imported cert: " + crt.getSubjectDN().getName());
						}
					}
				} catch (Exception e) {}
			}
		}
		log.info("Loaded " + trustKeyStore.size() + " trusted CA certificates.");
		trustManagerFactory = TrustManagerFactory.getInstance(TRUST_MANAGER_ALGORITHM);
		trustManagerFactory.init(trustKeyStore);
	}

	private String internalKeystorePassword = "";

	private final static String KEY_STORE_ALGORITHM = "JKS";

	private final static String TRUST_MANAGER_ALGORITHM = "SunX509";

	private final static String KEY_MANAGER_ALGORITHM = "SunX509";

	private List<Object> readObjectsFromFile(File file, final String password) throws IOException {
		PEMReader reader = null;
		PasswordFinder pfinder = new PasswordFinder() {

			@Override
			public char[] getPassword() {
				if (password != null) {
					return password.toCharArray();
				} else {
					return null;
				}
			}
		};
		List<Object> result = new ArrayList<Object>();
		try {
			reader = new PEMReader(new FileReader(file), pfinder);
			Object pemObject = null;
			while ((pemObject = reader.readObject()) != null) {
				result.add(pemObject);
			}
			return result;
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
	}

}
