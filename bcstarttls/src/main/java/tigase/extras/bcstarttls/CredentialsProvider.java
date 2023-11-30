package tigase.extras.bcstarttls;

import org.bouncycastle.tls.TlsServerContext;

/**
 * The CredentialsProvider interface defines the contract for classes that provide credentials for a TLS server context.
 */
public interface CredentialsProvider {

	/**
	 * Retrieves the credentials for a TLS server context.
	 *
	 * @param context the TLS server context
	 *
	 * @return the credentials for the provided TLS server context
	 */
	Credentials getCredentials(TlsServerContext context);

}
