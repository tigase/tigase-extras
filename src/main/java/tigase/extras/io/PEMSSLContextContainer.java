package tigase.extras.io;

//~--- non-JDK imports --------------------------------------------------------

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PasswordFinder;

import tigase.io.SSLContextContainerIfc;

//~--- JDK imports ------------------------------------------------------------

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import java.net.Socket;
import java.net.URLEncoder;

import java.security.InvalidAlgorithmParameterException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.CertPathTrustManagerParameters;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import tigase.io.SSLContextContainer;

//~--- classes ----------------------------------------------------------------

/**
 * Class description
 *
 *
 * @version        5.0.0, 2010.04.03 at 08:51:32 CEST
 * @author         Artur Hefczyc <artur.hefczyc@tigase.org>
 */
public class PEMSSLContextContainer implements SSLContextContainerIfc {
	private final static String KEY_MANAGER_ALGORITHM = "SunX509";
	private final static String KEY_STORE_ALGORITHM = "JKS";
	protected static final Logger log = Logger.getLogger(PEMSSLContextContainer.class.getName());

	/** Field description */
	public static final String PEM_PRIVATE_PWD_KEY = "pem-privatekey-password";

	/** Field description */
	public static final String PEM_PRIVATE_PWD_VAL = "";
	private final static String TRUST_MANAGER_ALGORITHM = "X509";
	private X509TrustManager[] tms = new X509TrustManager[] { new DummyTrustManager() };
	private SecureRandom secureRandom = new SecureRandom();

	//~--- constant enums -------------------------------------------------------

	private static enum TrustModel { all, selfsigned, trusted }

	//~--- fields ---------------------------------------------------------------

	/**
	 * Path to directory with CA certificates.<br/>In most Linux systems it may
	 * be: <code>/etc/ssl/certs</code>.
	 */
	private String caCertsPath = "";

	/**
	 * Used in SSL connections when domain is don't known yet
	 */
	private String defaultHostname = "default";

	// private String protocol = "ssl";

	/**
	 * Path to directory with private keys and certificate for domain.<br/> File
	 * name MUST have name in format <code><i>domainname</i>.pem</code>, for
	 * example:
	 * <ul>
	 * <li><code>tigase.org.pem</code>
	 * <li>
	 * <li><code>malkowscy.net.pem</code></li>
	 * </ul>
	 */
	private String domainKeysPath = "";
	private String internalKeystorePassword = "";

	/**
	 * When private keys are encrypted, they MUST be encrypted with one
	 * passphrase!
	 */
	private String privateKeyPassphrase = "";
	private Map<String, SSLContext> sslContexts = new HashMap<String, SSLContext>();
	private TrustModel trustModel = TrustModel.all;
	private KeyStore trustKeyStore;
	private TrustManagerFactory trustManagerFactory;

	//~--- constructors ---------------------------------------------------------

	/**
	 * Constructs ...
	 *
	 */
	public PEMSSLContextContainer() {
		Security.addProvider(new BouncyCastleProvider());
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param args
	 *
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		System.out.println(".");
		Security.addProvider(new BouncyCastleProvider());

		PEMSSLContextContainer x = new PEMSSLContextContainer();
		Map<String, Object> p = new HashMap<String, Object>();

		p.put(SERVER_CERTS_LOCATION_KEY, "./");

		// p.put(PEMSSLContextContainer.TRUSTED_CERTS_DIR_KEY, "/tmp/");
		p.put(ALLOW_SELF_SIGNED_CERTS_KEY, "true");
		x.init(p);

		SSLContext ctx = x.getSSLContext("tls", "malkowscy.net", false);
		Socket socket = ctx.getSocketFactory().createSocket("www.mbank.com.pl", 443);

		try {

			// Construct data
			String data = URLEncoder.encode("key1", "UTF-8") + "=" + URLEncoder.encode("value1", "UTF-8");

			data += "&" + URLEncoder.encode("key2", "UTF-8") + "=" + URLEncoder.encode("value2", "UTF-8");

			// Send header
			String path = "/";
			BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),
				"UTF8"));

			wr.write("POST " + path + " HTTP/1.0\r\n");
			wr.write("Content-Length: " + data.length() + "\r\n");
			wr.write("Content-Type: application/x-www-form-urlencoded\r\n");
			wr.write("\r\n");

			// Send data
			wr.write(data);
			wr.flush();

			// Get response
			BufferedReader rd = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String line;

			while ((line = rd.readLine()) != null) {

				// System.out.println(line);
			}

			wr.close();
			rd.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 *
	 * @param params
	 * {@inheritDoc}
	 */
	@Override
	public void addCertificates(Map<String, String> params) {
		sslContexts.clear();
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 *
	 * @param protocol
	 * @param hostname
	 *
	 * @return
	 * {@inheritDoc}
	 */
	@Override
	public SSLContext getSSLContext(String protocol, String hostname, boolean clientMode) {
		if (hostname == null) {
			hostname = defaultHostname;
		}

		try {
			String map_key = hostname + ":" + protocol;
			SSLContext sslContext = sslContexts.get(map_key);


			if ( clientMode ){
				sslContext = SSLContext.getInstance( protocol );
				sslContext.init( null, tms, secureRandom );
				return sslContext;
			}

			if (sslContext == null) {
				sslContext = SSLContext.getInstance(protocol);

				TrustManager[] trustManagers;

				switch (trustModel) {
					case all :
						trustManagers = new TrustManager[] { new DummyTrustManager() };
						log.fine("Creating SSLConext for " + protocol + ":" + hostname
								+ " with trust everyone model.");

						break;

					case selfsigned :
						trustManagers = new TrustManager[] { new SelfSignedTrustManager(trustKeyStore) };
						log.fine("Creating SSLConext for " + protocol + ":" + hostname
								+ " all valid pathes model.");

						break;

					case trusted :
						trustManagers = trustManagerFactory.getTrustManagers();
						log.fine("Creating SSLConext for " + protocol + ":" + hostname
								+ " trusted CA based model.");

						break;

					default :
						throw new RuntimeException("Unknown trust model: " + trustModel);
				}

				File[] path = new File[] {
					new File(new File(domainKeysPath).getAbsoluteFile() + File.separator + hostname + ".pem"),
					new File(new File(domainKeysPath).getAbsoluteFile() + File.separator + hostname + ".key"),
					new File(new File(domainKeysPath).getAbsoluteFile() + File.separator + hostname
						+ ".cer") };
				KeyStore keyStore = loadFromPEMFile(path, hostname, privateKeyPassphrase);
				KeyManagerFactory kmf = KeyManagerFactory.getInstance(KEY_MANAGER_ALGORITHM);

				kmf.init(keyStore, privateKeyPassphrase.toCharArray());
				sslContext.init(kmf.getKeyManagers(), trustManagers, new SecureRandom());
				sslContexts.put(map_key, sslContext);
			}

			return sslContext;
		} catch (Exception e) {
			log.log(Level.SEVERE, "Error on creating SSLContext for host " + hostname, e);

			return null;
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public KeyStore getTrustStore() {
		return trustKeyStore;
	}

	//~--- methods --------------------------------------------------------------

	/**
	 *
	 * @param params
	 * {@inheritDoc}
	 */
	@Override
	public void init(Map<String, Object> params) {
		this.caCertsPath = getFromMap(params, TRUSTED_CERTS_DIR_KEY, TRUSTED_CERTS_DIR_VAL);

		boolean allowInvalidCerts = Boolean.getBoolean(getFromMap(params, ALLOW_INVALID_CERTS_KEY,
			ALLOW_INVALID_CERTS_VAL));
		boolean allowSelfSignedCerts = "true".equals(getFromMap(params, ALLOW_SELF_SIGNED_CERTS_KEY,
			ALLOW_SELF_SIGNED_CERTS_VAL));

		if (allowInvalidCerts) {
			this.trustModel = TrustModel.all;
		} else {
			if (allowSelfSignedCerts) {
				this.trustModel = TrustModel.selfsigned;
			} else {
				this.trustModel = TrustModel.trusted;
			}
		}

		this.domainKeysPath = getFromMap(params, SERVER_CERTS_LOCATION_KEY, SERVER_CERTS_LOCATION_VAL);
		this.privateKeyPassphrase = getFromMap(params, PEM_PRIVATE_PWD_KEY, PEM_PRIVATE_PWD_VAL);
		this.defaultHostname = getFromMap(params, DEFAULT_DOMAIN_CERT_KEY, DEFAULT_DOMAIN_CERT_VAL);

		try {
			init();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Error on initialization", e);
		}
	}

	//~--- get methods ----------------------------------------------------------

	private String getFromMap(Map<String, Object> params, String key, String defaultVal) {
		if (params.containsKey(key)) {
			return (String) params.get(key);
		} else {
			params.put(key, defaultVal);

			return defaultVal;
		}
	}

	//~--- methods --------------------------------------------------------------

	private void init()
			throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException,
			InvalidAlgorithmParameterException {
		File root = new File(caCertsPath);
		File[] files = root.listFiles();

		trustKeyStore = KeyStore.getInstance(KEY_STORE_ALGORITHM);
		trustKeyStore.load(null, internalKeystorePassword.toCharArray());
		log.config("Initializing SSL Context Container with trust model = " + this.trustModel.name());
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

		X509CertSelector selector = new X509CertSelector();
		PKIXBuilderParameters cpp = new PKIXBuilderParameters(trustKeyStore, selector);

		cpp.setRevocationEnabled(false);

		CertPathTrustManagerParameters p = new CertPathTrustManagerParameters(cpp);

		trustManagerFactory.init(p);
	}

	private KeyStore loadFromPEMFile(File[] fileNames, String alias,
			final String privateKeyPassphrase)
			throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
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
		List<Certificate> certs = new ArrayList<Certificate>();
		Key key = null;

		log.info("Reading private key & certificate chain; alias: '" + alias + "', password: '"
				+ privateKeyPassphrase + "'");

		for (File fileName : fileNames) {
			if ( !fileName.exists()) {
				continue;
			}

			log.info("Reading data from file " + fileName);

			PEMReader reader = null;

			try {
				reader = new PEMReader(new FileReader(fileName), x);

				Object pemObject = null;

				while ((pemObject = reader.readObject()) != null) {
					if (pemObject instanceof Certificate) {
						certs.add((Certificate) pemObject);
					} else {
						if (pemObject instanceof KeyPair) {
							key = ((KeyPair) pemObject).getPrivate();
						} else {
							if (pemObject instanceof Key) {
								key = (Key) pemObject;
							}
						}
					}
				}
			} finally {
				if (reader != null) {
					reader.close();
				}
			}
		}

		if (certs.size() > 0) {
			KeyStore keyStore = KeyStore.getInstance(KEY_STORE_ALGORITHM);

			keyStore.load(null, internalKeystorePassword.toCharArray());
			keyStore.setKeyEntry(alias, key, internalKeystorePassword.toCharArray(),
					certs.toArray(new Certificate[] {}));

			return keyStore;
		} else {
			return loadFromPEMFile(new File[] {
				new File(new File(domainKeysPath).getAbsoluteFile() + File.separator
					+ "default.pem") }, alias, privateKeyPassphrase);
		}
	}

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

	//~--- inner classes --------------------------------------------------------

	private static class DummyTrustManager implements X509TrustManager {

		/**
		 * Method description
		 *
		 *
		 * @param x509CertificateArray
		 * @param string
		 *
		 * @throws CertificateException
		 */
		public void checkClientTrusted(final X509Certificate[] x509CertificateArray,
				final String string)
				throws CertificateException {}

		/**
		 * Method description
		 *
		 *
		 * @param x509CertificateArray
		 * @param string
		 *
		 * @throws CertificateException
		 */
		public void checkServerTrusted(final X509Certificate[] x509CertificateArray,
				final String string)
				throws CertificateException {}

		//~--- get methods --------------------------------------------------------

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}
	}


	private static class SelfSignedTrustManager implements X509TrustManager {
		private CertPathValidator certPathValidator;
		private KeyStore localTrustKeystore;
		private X509Certificate root;

		//~--- constructors -------------------------------------------------------

		/**
		 * Constructs ...
		 *
		 *
		 * @param trustKeystore
		 */
		public SelfSignedTrustManager(KeyStore trustKeystore) {
			try {
				this.localTrustKeystore = KeyStore.getInstance(KEY_STORE_ALGORITHM);

				ByteArrayOutputStream out = new ByteArrayOutputStream();

				trustKeystore.store(out, "".toCharArray());
				this.localTrustKeystore.load(new ByteArrayInputStream(out.toByteArray()), "".toCharArray());
				certPathValidator = CertPathValidator.getInstance(CertPathValidator.getDefaultType());
			} catch (Exception e) {
				log.log(Level.SEVERE, "Error on construct TrustManager", e);

				throw new RuntimeException("Error on construct TrustManager", e);
			}
		}

		//~--- methods ------------------------------------------------------------

		/**
		 * Method description
		 *
		 *
		 * @param chain
		 * @param authType
		 *
		 * @throws CertificateException
		 */
		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {}

		/**
		 * Method description
		 *
		 *
		 * @param chain
		 * @param authType
		 *
		 * @throws CertificateException
		 */
		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {
			for (X509Certificate certificate : chain) {
				if (certificate.getIssuerDN().equals(certificate.getSubjectDN())) {
					root = certificate;

					break;
				}
			}

			try {
				this.localTrustKeystore.setCertificateEntry("root", root);

				X509CertSelector selector = new X509CertSelector();
				PKIXBuilderParameters params = new PKIXBuilderParameters(this.localTrustKeystore, selector);

				params.setRevocationEnabled(false);

				List<X509Certificate> certList = Arrays.asList(chain);
				CertPath certPath = CertificateFactory.getInstance("X.509").generateCertPath(certList);

				certPathValidator.validate(certPath, params);
			} catch (Exception e) {
				throw new CertificateException(e);
			}
		}

		//~--- get methods --------------------------------------------------------

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return new X509Certificate[] { root };
		}
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
