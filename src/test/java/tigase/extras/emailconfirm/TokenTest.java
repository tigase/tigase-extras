package tigase.extras.emailconfirm;

import org.junit.Assert;
import org.junit.Test;
import tigase.util.Base64;
import tigase.xmpp.BareJID;

import java.util.Date;
import java.util.Random;

/**
 * Created by bmalkow on 21.04.2017.
 */
public class TokenTest {

	@Test
	public void testCopy() {
		byte[] buff = new byte[]{(byte) 97, (byte) 98, (byte) 99, (byte) 100};
		String s = Token.copy(buff, 1, 2);

		Assert.assertEquals("bc", s);
	}

	@Test
	public void testTokenCreation() {
		Token t = Token.create(BareJID.bareJIDInstanceNS("a@b.c"), new Date(100), "1234567890");
		Assert.assertEquals("AWFAYi5jADEwMAAxMjM0NTY3ODkw", t.getEncoded());
		Assert.assertEquals("lRCWbcS+WQPcGf60U6lhtTbuY2QUwHUCwWK7iUDyvMk=", t.getHash());

		t = Token.create(BareJID.bareJIDInstanceNS("a"), new Date(10), "X");
		byte[] buff = Base64.decode(t.getEncoded());

		Assert.assertEquals("Incorrect version", 1, buff[0]);
		Assert.assertEquals("Wrong lenght", 7, buff.length);
		Assert.assertEquals("Invalid JID section", 'a', buff[1]);
		Assert.assertEquals("Missing separator", 0, buff[2]);
		Assert.assertEquals("Invalid timestatmp section", '1', buff[3]);
		Assert.assertEquals("Invalid timestatmp section", '0', buff[4]);
		Assert.assertEquals("Missing separator", 0, buff[5]);
		Assert.assertEquals("Invalid random section", 'X', buff[6]);

		System.out.println(Token.create(BareJID.bareJIDInstanceNS("alamakota@tigase.org")).getEncoded());
	}

	@Test
	public void testTokenDecoding() {
		Token t = Token.parse("AWFAYi5jADEwMAAxMjM0NTY3ODkw");

		Assert.assertEquals(BareJID.bareJIDInstanceNS("a@b.c"), t.getJid());
		Assert.assertEquals(new Date(100), t.getTimestamp());
		Assert.assertEquals("1234567890", t.getRandom());

		Date d = new Date();
		String r = String.valueOf((new Random()).nextLong());
		t = Token.create(BareJID.bareJIDInstanceNS("alamakota@test.example.com"), d, r);
		System.out.println(t.getEncoded());

		Assert.assertEquals(BareJID.bareJIDInstanceNS("alamakota@test.example.com"), t.getJid());
		Assert.assertEquals(d, t.getTimestamp());
		Assert.assertEquals(r, t.getRandom());

	}

}