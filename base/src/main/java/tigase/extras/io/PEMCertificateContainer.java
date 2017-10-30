/*
 * PEMCertificateContainer.java
 *
 * Tigase Jabber/XMPP Server - Extras
 * Copyright (C) 2004-2017 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */
package tigase.extras.io;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PasswordFinder;
import tigase.cert.CertificateEntry;
import tigase.io.CertificateContainerIfc;
import tigase.kernel.beans.config.ConfigField;

import javax.net.ssl.*;
import java.io.*;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.io.SSLContextContainerIfc.*;

/**
 * Created by andrzej on 02.03.2016.
 */
public class PEMCertificateContainer
		implements CertificateContainerIfc {

	/** Field description */
	public static final String PEM_PRIVATE_PWD_KEY = "pem-privatekey-password";

	/** Field description */
	public static final String PEM_PRIVATE_PWD_VAL = "";
	protected static final Logger log = Logger.getLogger(PEMCertificateContainer.class.getName());
	private final static String KEY_MANAGER_ALGORITHM = "SunX509";
	private final static String KEY_STORE_ALGORITHM = "JKS";
	private final static String TRUST_MANAGER_ALGORITHM = "X509";

	/**
	 * Path to directory with CA certificates.<br/> In most Linux systems it may be: <code>/etc/ssl/certs</code>.
	 */
	@ConfigField(desc = "CA certs path", alias = TRUSTED_CERTS_DIR_KEY)
	private String caCertsPath = TRUSTED_CERTS_DIR_VAL;
	private TrustManager[] defTrustManagers = new X509TrustManager[]{new DummyTrustManager()};

	// private String protocol = "ssl";
	/**
	 * Used in SSL connections when domain is don't known yet
	 */
	private String defaultHostname = "default";
	/**
	 * Path to directory with private keys and certificate for domain.<br/> File name MUST have name in format
	 * <code><i>domainname</i>.pem</code>, for example: <ul> <li><code>tigase.org.pem</code> <li>
	 * <li><code>malkowscy.net.pem</code></li> </ul>
	 */
	@ConfigField(desc = "", alias = DEFAULT_DOMAIN_CERT_KEY)
	private String domainKeysPath = "";
	private String internalKeystorePassword = "";
	private Map<String, KeyManagerFactory> kmfs = new ConcurrentSkipListMap<>();
	/**
	 * When private keys are encrypted, they MUST be encrypted with one passphrase!
	 */
	@ConfigField(desc = "PEM Private Key password", alias = PEM_PRIVATE_PWD_KEY)
	private String privateKeyPassphrase = "";
	private KeyStore trustKeyStore;
	private TrustManagerFactory trustManagerFactory;
	@ConfigField(desc = "SSL certificate trust model", alias = "ssl-trust-model")
	private TrustModel trustModel = TrustModel.selfsigned;

	public PEMCertificateContainer() {
		Security.addProvider(new BouncyCastleProvider());
		setTrustModel(TrustModel.trusted);
	}

	@Override
	public void addCertificates(Map<String, String> map) throws CertificateParsingException {
		kmfs.clear();
	}

	@Override
	public KeyManager[] createCertificate(String s)
			throws NoSuchAlgorithmException, CertificateException, SignatureException, NoSuchProviderException,
				   InvalidKeyException, IOException, UnrecoverableKeyException, KeyStoreException {
		return new KeyManager[0];
	}

	@Override
	public CertificateEntry getCertificateEntry(String hostname) {
		String alias = hostname;
		if (alias == null) {
			alias = getDefCertAlias();
		}

		try {
			File[] path = new File[]{
					new File(new File(domainKeysPath).getAbsoluteFile() + File.separator + hostname + ".pem"),
					new File(new File(domainKeysPath).getAbsoluteFile() + File.separator + hostname + ".key"),
					new File(new File(domainKeysPath).getAbsoluteFile() + File.separator + hostname + ".cer")};
			KeyStore keyStore = loadFromPEMFile(path, hostname, privateKeyPassphrase);

			KeyStore.PrivateKeyEntry e = (KeyStore.PrivateKeyEntry) keyStore.getEntry(hostname, null);

			ArrayList<Certificate> cc = new ArrayList<>();
			if (e.getCertificate() != null) {
				cc.add(e.getCertificate());
			}
			if (e.getCertificateChain() != null) {
				cc.addAll(Arrays.asList(e.getCertificateChain()));
			}

			CertificateEntry c = new CertificateEntry();
			c.setPrivateKey(e.getPrivateKey());
			c.setCertChain(cc.toArray(new Certificate[]{}));

			return c;
		} catch (Exception ex) {
			throw new RuntimeException("Could not load certificate for domain " + hostname);
		}
	}

	@Override
	public String getDefCertAlias() {
		return defaultHostname;
	}

	@Override
	public KeyManager[] getKeyManagers(String hostname) {
		KeyManagerFactory kmf = this.kmfs.get(hostname);
		if (kmf != null) {
			return kmf.getKeyManagers();
		}

		try {
			File[] path = new File[]{
					new File(new File(domainKeysPath).getAbsoluteFile() + File.separator + hostname + ".pem"),
					new File(new File(domainKeysPath).getAbsoluteFile() + File.separator + hostname + ".key"),
					new File(new File(domainKeysPath).getAbsoluteFile() + File.separator + hostname + ".cer")};
			KeyStore keyStore = loadFromPEMFile(path, hostname, privateKeyPassphrase);
			kmf = KeyManagerFactory.getInstance(KEY_MANAGER_ALGORITHM);

			kmf.init(keyStore, privateKeyPassphrase.toCharArray());
			this.kmfs.put(hostname, kmf);
			return kmf.getKeyManagers();
		} catch (Exception ex) {
			throw new RuntimeException("Could not load certificate for domain " + hostname);
		}
	}

	@Override
	public TrustManager[] getTrustManagers() {
		return defTrustManagers;
	}

	@Override
	public KeyStore getTrustStore() {
		return trustKeyStore;
	}

	@Override
	public void init(Map<String, Object> params) {
		this.caCertsPath = getFromMap(params, TRUSTED_CERTS_DIR_KEY, TRUSTED_CERTS_DIR_VAL);

		boolean allowInvalidCerts = Boolean.getBoolean(
				getFromMap(params, ALLOW_INVALID_CERTS_KEY, ALLOW_INVALID_CERTS_VAL));
		boolean allowSelfSignedCerts = "true".equals(
				getFromMap(params, ALLOW_SELF_SIGNED_CERTS_KEY, ALLOW_SELF_SIGNED_CERTS_VAL));

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

		setTrustModel(trustModel);
	}

	public void setTrustModel(TrustModel model) {
		this.trustModel = model;
		switch (trustModel) {
			case all:
				defTrustManagers = new TrustManager[]{new DummyTrustManager()};
				break;

			case selfsigned:
				defTrustManagers = new TrustManager[]{new SelfSignedTrustManager(trustKeyStore)};
				break;

			case trusted:
				defTrustManagers = trustManagerFactory.getTrustManagers();
				break;

			default:
				throw new RuntimeException("Unknown trust model: " + trustModel);
		}
	}

	private String getFromMap(Map<String, Object> params, String key, String defaultVal) {
		if (params.containsKey(key)) {
			return (String) params.get(key);
		} else {
			params.put(key, defaultVal);

			return defaultVal;
		}
	}

	private void init() throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException,
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
				} catch (Exception e) {
				}
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

	private KeyStore loadFromPEMFile(File[] fileNames, String alias, final String privateKeyPassphrase)
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
		List<java.security.cert.Certificate> certs = new ArrayList<java.security.cert.Certificate>();
		Key key = null;

		log.info("Reading private key & certificate chain; alias: '" + alias + "', password: '" + privateKeyPassphrase +
						 "'");
		for (File fileName : fileNames) {
			if (!fileName.exists()) {
				continue;
			}
			log.info("Reading data from file " + fileName);

			PEMReader reader = null;

			try {
				reader = new PEMReader(new FileReader(fileName), x);

				Object pemObject = null;

				while ((pemObject = reader.readObject()) != null) {
					if (pemObject instanceof java.security.cert.Certificate) {
						certs.add((java.security.cert.Certificate) pemObject);
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
								 certs.toArray(new java.security.cert.Certificate[]{}));

			return keyStore;
		} else {
			return loadFromPEMFile(
					new File[]{new File(new File(domainKeysPath).getAbsoluteFile() + File.separator + "default.pem")},
					alias, privateKeyPassphrase);
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

	private static enum TrustModel {
		all,
		selfsigned,
		trusted
	}

	//~--- inner classes --------------------------------------------------------

	private static class DummyTrustManager
			implements X509TrustManager {

		@Override
		public void checkClientTrusted(final X509Certificate[] x509CertificateArray, final String string)
				throws CertificateException {
		}

		@Override
		public void checkServerTrusted(final X509Certificate[] x509CertificateArray, final String string)
				throws CertificateException {
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}
	}

	private static class SelfSignedTrustManager
			implements X509TrustManager {

		private CertPathValidator certPathValidator;
		private KeyStore localTrustKeystore;
		private X509Certificate root;

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

		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
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

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return new X509Certificate[]{root};
		}
	}

}
