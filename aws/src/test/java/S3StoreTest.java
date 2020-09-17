/*
 * Tigase Server Extras for AWS - Extra modules to Tigase Server
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
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import tigase.extras.http.upload.S3Store;
import tigase.xmpp.jid.BareJID;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

@Ignore
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class S3StoreTest {

	private static S3Store store;
	private static BareJID uploaderJid;
	private static String slotId;
	private static String filename;
	private static byte[] data;

	@BeforeClass
	public static void setup() throws NoSuchFieldException, IllegalAccessException, NoSuchAlgorithmException {
		uploaderJid = BareJID.bareJIDInstanceNS(UUID.randomUUID().toString(), "test.com");
		slotId = UUID.randomUUID().toString();
		filename = generateFilename("test-");
		data = generateData();

		Logger.getGlobal().setLevel(Level.ALL);
		store = new S3Store();
		Field f = S3Store.class.getDeclaredField("bucket");
		f.setAccessible(true);
		f.set(store, "test-bucket-andrzej1");
		f = S3Store.class.getDeclaredField("autocreateBucket");
		f.setAccessible(true);
		f.set(store, true);
		store.beanConfigurationChanged(Collections.emptyList());
	}

	@Test
	public void test1_upload() throws IOException {
		store.setContent(uploaderJid, slotId, filename, data.length, Channels.newChannel(new ByteArrayInputStream(data)));
	}

	@Test
	public void test2_download() throws IOException {
		ByteArrayOutputStream boas = new ByteArrayOutputStream();
		WritableByteChannel tmp = Channels.newChannel(boas);
		ReadableByteChannel in = store.getContent(uploaderJid, slotId, filename);
		int read = 0;
		while (in.isOpen() && read >= 0) {
			ByteBuffer buffer = ByteBuffer.allocate(1024);
			read = in.read(buffer);
			buffer.flip();
			tmp.write(buffer);
		}
		assertEquals(data.length, boas.size());
		assertArrayEquals(data, boas.toByteArray());
	}

	@Test
	public void test3_countFiles() throws IOException {
		assertEquals(1, store.count());
	}

	@Test
	public void test4_countSize() throws IOException {
		assertEquals(data.length, store.size());
	}

	@Test
	public void test5_delete() throws IOException {
		store.remove(uploaderJid, slotId);
		assertEquals(0, store.count());
	}
	
	static byte[] generateData() throws NoSuchAlgorithmException {
		SecureRandom random = SecureRandom.getInstanceStrong();
		byte[] data = new byte[random.nextInt(100 * 1024) + random.nextInt(1024)];
		random.nextBytes(data);
		return data;
	}

	static String generateFilename(String prefix) throws NoSuchAlgorithmException {
		StringBuilder sb = new StringBuilder(prefix);
		SecureRandom random = SecureRandom.getInstanceStrong();
		int limit = 8 + random.nextInt(8);
		return random.ints(48, 123)
				.filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
				.limit(limit)
				.collect(() -> sb, StringBuilder::appendCodePoint, StringBuilder::append)
				.toString();
	}
}
