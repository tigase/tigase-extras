/*
 * Tigase Server Extras LDAP Server - Extra modules to Tigase Server
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

import com.unboundid.asn1.ASN1Buffer;
import com.unboundid.asn1.ASN1StreamReader;
import com.unboundid.ldap.protocol.LDAPMessage;
import com.unboundid.ldap.sdk.LDAPException;
import tigase.io.IOInterface;
import tigase.ldap.processors.LDAPSession;
import tigase.net.IOService;
import tigase.xmpp.jid.BareJID;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LdapIOService<RefObject> extends IOService<RefObject> implements LDAPSession {

	private static final Logger log = Logger.getLogger(LdapIOService.class.getName());

	private final PipedInputStream pipedInputStream;
	private final PipedOutputStream pipedOutputStream;
	private final ASN1StreamReader reader;

	private ConcurrentLinkedQueue<LDAPMessage> receivedRequests = new ConcurrentLinkedQueue<>();
	private ConcurrentLinkedQueue<LDAPMessage> waitingResponses = new ConcurrentLinkedQueue<>();

	private LdapConnectionManager connectionManager;
	private BareJID authorizedJID;
	public ReentrantLock writeInProgress = new ReentrantLock();

	public LdapIOService() throws IOException {
		pipedInputStream = new PipedInputStream();
		pipedOutputStream = new PipedOutputStream(pipedInputStream);
		reader = new ASN1StreamReader(pipedInputStream);
	}

	@Override
	public BareJID getAuthorizedJID() {
		return authorizedJID;
	}

	@Override
	public void setAuthorizedJID(BareJID jid) {
		authorizedJID = jid;
	}

	public void setConnectionManager(LdapConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
	}

	public Queue<LDAPMessage> getReceivedRequests() {
		return receivedRequests;
	}

	@Override
	public IOService<?> call() throws IOException {
		IOService<?> io = super.call();
		if (isConnected() && !waitingResponses.isEmpty()) {
				processWaitingPackets();
		}
		return io;
	}

	@Override
	public void processWaitingPackets() throws IOException {
		// nothing to do..
		LDAPMessage resp;
		while ((resp = waitingResponses.poll()) != null) {
			writeMessage(resp);
		}
	}
	
	private void writeMessage(LDAPMessage message) throws IOException {
		log.finest(() -> "sending response message with id = " + message.getMessageID() + ", operator = " + message.getProtocolOp());
		ASN1Buffer buffer = new ASN1Buffer();
		message.writeTo(buffer);
		writeBytes(buffer.asByteBuffer());
	}

	public void sendResponse(LDAPMessage response) {
		waitingResponses.add(response);
	}

	private int bytesRead() {
		try {
			Field f = IOService.class.getDeclaredField("socketIO");
			f.setAccessible(true);
			IOInterface io = (IOInterface) f.get(this);
			return io.bytesRead();
		} catch (NoSuchFieldException | IllegalAccessException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	protected void processSocketData() throws IOException {
		if (isConnected()) {
			ByteBuffer buf = readBytes();
			
			while (isConnected() && (buf != null) && (buf.hasRemaining() || bytesRead() > 0)) {
				int remaining = buf.remaining();
				log.finest(() -> "read " + remaining + " bytes");
				byte[] data = new byte[remaining];
				buf.get(data);
				log.finest(() -> "wrote " + data.length + " bytes to the pipe");
				pipedOutputStream.write(data);
				buf.compact();
				buf = readBytes();
			}

			try {
				if (pipedInputStream.available() > 0) {
					log.finest(() -> "reading data from reader...");
					LDAPMessage message = LDAPMessage.readFrom(reader, true);
					log.finest(() -> "received message " + message);
					if (message != null) {
						receivedRequests.add(message);
					}
					log.finest(() -> "reading data from reader completed.");
				}
			} catch (LDAPException ex) {
				log.log(Level.SEVERE, "LDAPException", ex);
				throw new IOException(ex);
			}
		}
	}

	@Override
	protected int receivedPackets() {
		return receivedRequests.size();
	}
}
