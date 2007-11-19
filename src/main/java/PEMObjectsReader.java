import java.io.FileReader;
import java.io.IOException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PasswordFinder;

public class PEMObjectsReader {

	public static KeyStore loadFromPEMFile(String fileName, String alias, final String privateKeyPassphrase)
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
			KeyStore keyStore = KeyStore.getInstance("JKS");
			keyStore.load(null, "".toCharArray());

			keyStore.setKeyEntry(alias, key, "".toCharArray(), certs.toArray(new Certificate[] {}));

			return keyStore;
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
	}

	public static void main(String[] args) throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		KeyStore ks = loadFromPEMFile("dd.cer", "alias", "1234");

		
	}

}
