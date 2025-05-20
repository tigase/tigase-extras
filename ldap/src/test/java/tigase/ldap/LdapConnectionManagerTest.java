/*
 * Tigase Server Extras MongoDB - Extra modules to Tigase Server
 * Copyright (C) 2007 Tigase, Inc. (office@tigase.com)
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
package tigase.ldap;

import com.unboundid.ldap.sdk.LDAPException;
import org.junit.*;
import tigase.TestLogger;
import tigase.component.DSLBeanConfigurator;
import tigase.component.DSLBeanConfiguratorWithBackwardCompatibility;
import tigase.conf.ConfigHolder;
import tigase.conf.LoggingBean;
import tigase.db.TigaseDBException;
import tigase.eventbus.EventBusFactory;
import tigase.io.CertificateContainer;
import tigase.io.SSLContextContainer;
import tigase.kernel.AbstractKernelWithUserRepositoryTestCase;
import tigase.kernel.DefaultTypesConverter;
import tigase.kernel.beans.config.AbstractBeanConfigurator;
import tigase.kernel.core.Kernel;
import tigase.net.SocketType;
import tigase.vhosts.DummyVHostManager;
import tigase.xmpp.jid.BareJID;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.StartTlsRequest;
import javax.naming.ldap.StartTlsResponse;
import javax.net.ssl.*;
import java.io.IOException;
import java.io.StringWriter;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LdapConnectionManagerTest extends AbstractKernelWithUserRepositoryTestCase {

	static Logger log;

	public LdapConnectionManager ldapManager;

	private static void dumpConfiguration(DSLBeanConfigurator config) throws IOException {
		final StringWriter stringWriter = new StringWriter();

		config.dumpConfiguration(stringWriter);
		log.fine("Configuration dump:" + stringWriter.toString());
	}

	@Override
	protected void registerBeans(Kernel kernel) {
		super.registerBeans(kernel);
		kernel.registerBean(DefaultTypesConverter.class).exportable().exec();
		kernel.registerBean(DSLBeanConfiguratorWithBackwardCompatibility.class).exportable().exec();
		kernel.registerBean("eventBus").asInstance(EventBusFactory.getInstance()).exportable().exec();

		DSLBeanConfigurator configurator = kernel.getInstance(DSLBeanConfigurator.class);
		configurator.setConfigHolder(new ConfigHolder());
		Map<String,Object> ldap = new HashMap<>();
		Map<String,Object> connections = new HashMap<>();
		ldap.put("connections", connections);
		Map<String,Object> portSSL = new HashMap<>();
		AbstractBeanConfigurator.BeanDefinition beanDefinition = new AbstractBeanConfigurator.BeanDefinition();
		beanDefinition.setBeanName("10489");
		beanDefinition.setActive(true);
		beanDefinition.put("ifc", "*");
		beanDefinition.put("socket", SocketType.ssl);
		connections.put("10489", beanDefinition);
		configurator.getConfigHolder().getProperties().put("ldap", ldap);

		kernel.registerBean(CertificateContainer.class).exportable().exec();
		kernel.registerBean(SSLContextContainer.class).exportable().setActive(true).exec();
		kernel.registerBean("vhost-man").asClass(DummyVHostManager.class).exportable().setActive(true).exec();
		kernel.registerBean(LdapConnectionManager.class).setActive(true).exec();
		kernel.registerBean("logging").asClass(LoggingBean.class).setActive(true).setPinned(true).exec();
	}
	
	@Before
	public void setup() throws TigaseDBException {
		log = Logger.getLogger("tigase");
		TestLogger.configureLogger(log, Level.CONFIG);

		getAuthRepository().addUser(BareJID.bareJIDInstanceNS("tygrys", "tigase.org"), "12345");
		getAuthRepository().addUser(BareJID.bareJIDInstanceNS("admin", "tigase.org"), "12345admin");
		getInstance(DummyVHostManager.class).addVhost("tigase.org");
		getInstance(DummyVHostManager.class).getVHostItem("tigase.org").setAdmins(new String[]{"admin@tigase.org"});

		try {
			final SSLContextContainer context = getInstance(SSLContextContainer.class);
			final LoggingBean loggingBean = getInstance(LoggingBean.class);
			loggingBean.setPacketFullDebug(true);
			ldapManager = getInstance(LdapConnectionManager.class);
			ldapManager.start();
			DSLBeanConfigurator configurator = getInstance(DSLBeanConfigurator.class);
			dumpConfiguration(configurator);
		} catch (Exception ex) {
			log.log(Level.WARNING, ex, () -> "There was an error setting up test");
		}

		TestLogger.configureLogger(log, Level.ALL);
	}

	@After
	public void tearDown() {
		if (ldapManager != null) {
			ldapManager.stop();
			ldapManager = null;
		}
	}

	@Test
	public void testSimpleAuthSuccess_SSL()
			throws  NamingException {
		Hashtable<String, String> environment = new Hashtable<String, String>();

		environment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		environment.put(Context.PROVIDER_URL, "ldaps://localhost:10489");
		environment.put(Context.SECURITY_AUTHENTICATION, "simple");
		environment.put(Context.SECURITY_PRINCIPAL, "cn=tygrys,ou=Users,dc=tigase,dc=org");
		environment.put(Context.SECURITY_CREDENTIALS, "12345");
		environment.put("java.naming.ldap.factory.socket", "tigase.ldap.MySSLSocketFactory");

		log.finest("Authenticating...");
		LdapContext context = new InitialLdapContext(environment, null);
		log.finest("Authenticated successfully!");
		context.close();
	}

	@Test
	@Ignore
	public void testSTARTLS_NoAuth()
			throws InterruptedException, NamingException, LDAPException, IOException, NoSuchAlgorithmException,
				   KeyManagementException {
		Hashtable<String, String> environment = new Hashtable<String, String>();

		environment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		environment.put(Context.PROVIDER_URL, "ldap://localhost:10389");

		log.finest("Authenticating...");
		LdapContext context = new InitialLdapContext(environment, null);
		StartTlsResponse tls = (StartTlsResponse) context.extendedOperation(new StartTlsRequest());
		SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(null, new TrustManager[]{
				new DummyTrustManager()
		}, new SecureRandom());
		sslContext.getDefaultSSLParameters().setServerNames(List.of(new SNIHostName("localhost")));
		SSLSocketFactory factory = sslContext.getSocketFactory();
		tls.setHostnameVerifier(new HostnameVerifier() {
			@Override
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		});
		tls.negotiate(factory);
		log.finest("Authenticated successfully!");
		context.close();
	}

	@Test
	public void testSimpleAuthFailureWrongPassword() throws NamingException {
		Hashtable<String, String> environment = new Hashtable<String, String>();

		environment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		environment.put(Context.PROVIDER_URL, "ldap://localhost:10389");
		environment.put(Context.SECURITY_AUTHENTICATION, "simple");
		environment.put(Context.SECURITY_PRINCIPAL, "cn=tygrys,ou=Users,dc=tigase,dc=org");
		environment.put(Context.SECURITY_CREDENTIALS, "123456");

		boolean authenticated = false;
		try {
			log.finest("Authenticating...");
			DirContext context = new InitialDirContext(environment);
			log.finest("Authenticated successfully!");
			authenticated = true;
			context.close();
		} catch (AuthenticationException ex) {
			authenticated = false;
		}
		Assert.assertFalse("authentication succeeded with wrong password", authenticated);
	}

	@Test
	public void testSimpleAuthFailureNotExistingUser() throws NamingException {
		Hashtable<String, String> environment = new Hashtable<String, String>();

		environment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		environment.put(Context.PROVIDER_URL, "ldap://localhost:10389");
		environment.put(Context.SECURITY_AUTHENTICATION, "simple");
		environment.put(Context.SECURITY_PRINCIPAL, "cn=tygrys-missing,ou=Users,dc=tigase,dc=org");
		environment.put(Context.SECURITY_CREDENTIALS, "12345");

		boolean authenticated = false;
		try {
			log.finest("Authenticating...");
			DirContext context = new InitialDirContext(environment);
			log.finest("Authenticated successfully!");
			authenticated = true;
			context.close();
		} catch (AuthenticationException ex) {
			authenticated = false;
		}
		Assert.assertFalse("authentication succeeded for not existing user", authenticated);
	}

	@Test
	public void testSimpleSimpleAuthWithQuery() throws InterruptedException, NamingException {
		Hashtable<String, String> environment = new Hashtable<String, String>();

		environment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		environment.put(Context.PROVIDER_URL, "ldap://localhost:10389");
		environment.put(Context.SECURITY_AUTHENTICATION, "simple");
		environment.put(Context.SECURITY_PRINCIPAL, "cn=tygrys,ou=Users,dc=tigase,dc=org");
		environment.put(Context.SECURITY_CREDENTIALS, "12345");

		log.finest("Authenticating...");
		DirContext context = new InitialDirContext(environment);
		log.finest("Authenticated successfully!");
		log.finest("executing query...");
		NamingEnumeration<SearchResult> enumeration = context.search("ou=Users,dc=tigase,dc=org", "(&(objectClass=posixAccount)(cn=tygrys))", null);
		boolean found = false;
		while (enumeration.hasMore()) {
			SearchResult result = enumeration.next();
			Attributes attrs = result.getAttributes();

			log.finest("search result = " + result.toString());
			Assert.assertEquals("cn=tygrys,ou=Users,dc=tigase,dc=org", result.getNameInNamespace());
			Assert.assertEquals("tygrys", attrs.get("cn").get().toString());
			Assert.assertEquals("tygrys@tigase.org", attrs.get("mail").get().toString());
			Assert.assertEquals("tygrys@tigase.org", attrs.get("xmpp").get().toString());
			found = true;
		}
		Assert.assertTrue(found);
		found = false;
		enumeration = context.search("ou=Users,dc=tigase,dc=org", "(&(objectClass=posixAccount)(mail=tygrys@tigase.org))", null);
		while (enumeration.hasMore()) {
			SearchResult result = enumeration.next();
			Attributes attrs = result.getAttributes();

			log.finest("search result = " + result.toString());
			Assert.assertEquals("cn=tygrys,ou=Users,dc=tigase,dc=org", result.getNameInNamespace());
			Assert.assertEquals("tygrys", attrs.get("cn").get().toString());
			Assert.assertEquals("tygrys@tigase.org", attrs.get("mail").get().toString());
			Assert.assertEquals("tygrys@tigase.org", attrs.get("xmpp").get().toString());
			found = true;
		}
		Assert.assertTrue(found);
		context.close();
	}

	@Test
	public void testSimpleSimpleAuthWithGroups() throws InterruptedException, NamingException {
		Hashtable<String, String> environment = new Hashtable<String, String>();

		environment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		environment.put(Context.PROVIDER_URL, "ldap://localhost:10389");
		environment.put(Context.SECURITY_AUTHENTICATION, "simple");
		environment.put(Context.SECURITY_PRINCIPAL, "cn=tygrys,ou=Users,dc=tigase,dc=org");
		environment.put(Context.SECURITY_CREDENTIALS, "12345");

		log.finest("Authenticating...");
		DirContext context = new InitialDirContext(environment);
		log.finest("Authenticated successfully!");
		log.finest("executing query...");
		NamingEnumeration<SearchResult> enumeration = context.search("ou=Users,dc=tigase,dc=org", "(&(objectClass=posixAccount)(cn=tygrys))", null);
		boolean found = false;
		while (enumeration.hasMore()) {
			SearchResult result = enumeration.next();
			Attributes attrs = result.getAttributes();

			log.finest("search result = " + result.toString());
			Assert.assertEquals("cn=tygrys,ou=Users,dc=tigase,dc=org", result.getNameInNamespace());
			Assert.assertEquals("tygrys", attrs.get("cn").get().toString());
			Assert.assertEquals("tygrys@tigase.org", attrs.get("mail").get().toString());
			Assert.assertEquals("tygrys@tigase.org", attrs.get("xmpp").get().toString());
			List<String> groups = getGroupsFromUserAttribute(attrs, "memberOfGid");
			assertCollectionsEqual(List.of("Users"), groups);
			groups = getGroupsFromUserAttribute(attrs, "memberOf");
			assertCollectionsEqual(List.of("cn=Users,ou=Groups,dc=tigase,dc=org"), groups);
			found = true;
		}
		Assert.assertTrue(found);
		found = false;
		enumeration = context.search("ou=Users,dc=tigase,dc=org", "(&(objectClass=posixAccount)(mail=tygrys@tigase.org))", null);
		while (enumeration.hasMore()) {
			SearchResult result = enumeration.next();
			Attributes attrs = result.getAttributes();

			log.finest("search result = " + result.toString());
			Assert.assertEquals("cn=tygrys,ou=Users,dc=tigase,dc=org", result.getNameInNamespace());
			Assert.assertEquals("tygrys", attrs.get("cn").get().toString());
			Assert.assertEquals("tygrys@tigase.org", attrs.get("mail").get().toString());
			Assert.assertEquals("tygrys@tigase.org", attrs.get("xmpp").get().toString());
			found = true;
		}
		Assert.assertTrue(found);

		Set<String> expectedInNamespaces = Stream.of("Users")
				.map(name -> "cn=" + name + ",ou=Groups,dc=tigase,dc=org")
				.collect(Collectors.toSet());
		List<String> gotInNamespaces = getGroupsByFilter(context, null, "memberuid", "tygrys");
		assertCollectionsEqual(expectedInNamespaces, gotInNamespaces);

		expectedInNamespaces = Stream.of("Users")
				.map(name -> "cn=" + name + ",ou=Groups,dc=tigase,dc=org")
				.collect(Collectors.toSet());
		gotInNamespaces = getGroupsByFilter(context, null, "member", "uid=tygrys,ou=Users,dc=tigase,dc=org");
		assertCollectionsEqual(expectedInNamespaces, gotInNamespaces);

		expectedInNamespaces = Collections.emptySet();
		gotInNamespaces = getGroupsByFilter(context, "(|(cn=Users)(cn=Administrators))", "memberuid", "admin");
		assertCollectionsEqual(expectedInNamespaces, gotInNamespaces);

		context.close();

		environment.put(Context.SECURITY_PRINCIPAL, "cn=admin,ou=Users,dc=tigase,dc=org");
		environment.put(Context.SECURITY_CREDENTIALS, "12345admin");
		context = new InitialDirContext(environment);

		expectedInNamespaces = Stream.of("Administrators", "Users")
				.map(name -> "cn=" + name + ",ou=Groups,dc=tigase,dc=org")
				.collect(Collectors.toSet());
		gotInNamespaces = getGroupsByFilter(context, "(|(cn=Users)(cn=Administrators))", "memberuid", "admin");
		assertCollectionsEqual(expectedInNamespaces, gotInNamespaces);
		context.close();
	}

	private List<String> getGroupsFromUserAttribute(Attributes attrs, String groupAttributeName)
			throws NamingException {
		Attribute attribute = attrs.get(groupAttributeName);
		Assert.assertNotNull(attribute);
		List<String> groups = new ArrayList<>();
		for (NamingEnumeration<?> e = attribute.getAll(); e.hasMore();) {
			groups.add(e.next().toString());
		}
		return groups;
	}

	private List<String> getGroupsByFilter(DirContext context, String groupsFilter, String memberAttribute, String uid)
			throws NamingException {
		String filter = "(" + memberAttribute + "=" + uid + ")";
		if (groupsFilter != null) {
			filter = "(&" + groupsFilter + filter + ")";
		}
		List<String> groups = new ArrayList<>();
		NamingEnumeration<SearchResult> enumeration = context.search("ou=Groups,dc=tigase,dc=org", filter, null);
		while (enumeration.hasMore()) {
			SearchResult result = enumeration.next();
			groups.add(result.getNameInNamespace());
		}
		return groups;
	}

	@Test
	public void testSaslAuth_NoSupport() throws InterruptedException, NamingException {
		DirContext context = new InitialDirContext();
		Attributes attrs = context.getAttributes("ldap://localhost:10389", new String[]{"supportedSASLMechanisms"});
		Assert.assertEquals(0, attrs.size());
		context.close();
	}

	@Test
	@Ignore
	public void testLongRun() throws InterruptedException {
		Thread.sleep(Duration.ofHours(1));
	}

	private <T> void assertCollectionsEqual(Collection<T> expected, Collection<T> actual) {
		assertCollectionsEqual(null, expected, actual);
	}

	private <T> void assertCollectionsEqual(String message, Collection<T> expected, Collection<T> actual) {
		List<T> unexpected = actual.stream().filter(Predicate.not(expected::contains)).toList();
		List<T> missing = expected.stream().filter(Predicate.not(actual::contains)).toList();
		if ((!missing.isEmpty()) || (!unexpected.isEmpty())) {
			Assert.fail((message == null ? "Values should be equal." : message) + " missing items: " + missing + ", unexpected items: " + unexpected);
		}
	}
}