/*
 * Tigase Server Extras Bouncycastle for StartTLS - Extra modules to Tigase Server
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
package tigase.extras.bcstarttls;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.tls.Certificate;
import org.bouncycastle.tls.TlsServerProtocol;
import org.bouncycastle.tls.crypto.TlsCertificate;
import org.bouncycastle.tls.crypto.impl.bc.BcTlsCrypto;
import tigase.cert.CertCheckResult;
import tigase.cert.CertificateUtil;
import tigase.io.*;
import tigase.stats.StatisticsList;

import javax.net.ssl.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BcTLSIO
		implements IOInterface, TLSIOIfc {

	public static final String TLS_CAPS = "tls-caps";
	private static final Logger log = Logger.getLogger(BcTLSIO.class.getName());
	private final TrustManager[] clientTrustManagers;
	private final boolean needClientAuth;
	private final DefaultTls13Server server;
	private final TlsServerProtocol serverProtocol;
	private final boolean wantClientAuth;
	private int bytesRead = 0;
	private boolean handshakeCompleted = false;
	private IOInterface io = null;
	private Certificate peerCertificate;
	private byte[] tlsExporter;
	/**
	 * <code>tlsInput</code> buffer keeps data decoded from tlsWrapper.
	 */
	private ByteBuffer tlsInput = null;
	private byte[] tlsUnique;

	private final TLSWrapper fakeWrapper = new TLSWrapper() {
		@Override
		public int bytesConsumed() {
//			(new RuntimeException("DEBUG")).printStackTrace();
			throw new RuntimeException("Cannot be used!");
		}

		@Override
		public void close() throws SSLException {
			try {
				BcTLSIO.this.serverProtocol.close();
			} catch (IOException e) {
				log.log(Level.FINE, "Cannot close Server Protocol", e);
			}
		}

		@Override
		public int getAppBuffSize() {
			throw new RuntimeException("Cannot be used!");
		}

		@Override
		public CertCheckResult getCertificateStatus(boolean revocationEnabled,
													SSLContextContainerIfc sslContextContainer) {
			java.security.cert.Certificate[] chain;
			try {
				chain = getPeerCertificates();
			} catch (SSLPeerUnverifiedException e) {
				return CertCheckResult.none;
			}

			if (chain == null || chain.length == 0) {
				return CertCheckResult.none;
			}

			try {
				return CertificateUtil.validateCertificate(chain, sslContextContainer.getTrustStore(),
														   revocationEnabled);
			} catch (Exception ex) {
				log.log(Level.WARNING, "Problem validating certificate", ex);
			}

			return CertCheckResult.invalid;
		}

		@Override
		public SSLEngineResult.HandshakeStatus getHandshakeStatus() {
			if (handshakeCompleted) {
				return SSLEngineResult.HandshakeStatus.FINISHED;
			} else {
				return SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING;
			}
		}

		@Override
		public java.security.cert.Certificate[] getLocalCertificates() {
			return gen(BcTLSIO.this.server.getLocalCertificates());
		}

		@Override
		public int getNetBuffSize() {
			throw new RuntimeException("Cannot be used!");
		}

		@Override
		public int getPacketBuffSize() {
			throw new RuntimeException("Cannot be used!");
		}

		@Override
		public java.security.cert.Certificate[] getPeerCertificates() throws SSLPeerUnverifiedException {
			if (BcTLSIO.this.peerCertificate == null) {
				return null;
			}

			return gen(BcTLSIO.this.peerCertificate);
		}

		@Override
		public TLSStatus getStatus() {
			return TLSStatus.OK;
		}

		@Override
		public byte[] getTlsExporterBindingData() {
			return BcTLSIO.this.tlsExporter;
		}

		@Override
		public byte[] getTlsUniqueBindingData() {
			return BcTLSIO.this.tlsUnique;
		}

		@Override
		public boolean isClientMode() {
			return false;
		}

		@Override
		public boolean isNeedClientAuth() {
			return BcTLSIO.this.needClientAuth;
		}

		@Override
		public void setDebugId(String id) {
			throw new RuntimeException("Cannot be used!");
		}

		@Override
		public ByteBuffer unwrap(ByteBuffer net, ByteBuffer app) throws SSLException {
			throw new RuntimeException("Cannot be used!");
		}

		@Override
		public boolean wantClientAuth() {
			return BcTLSIO.this.wantClientAuth;
		}

		@Override
		public void wrap(ByteBuffer app, ByteBuffer net) throws SSLException {
			throw new RuntimeException("Cannot be used!");
		}
	};

	public BcTLSIO(final CertificateContainerIfc certificateContainer, final TLSEventHandler eventHandler,
				   final IOInterface ioi, String hostname, final ByteOrder order, boolean wantClientAuth,
				   boolean needClientAuth, TrustManager[] x509TrustManagers) throws IOException {
		this.clientTrustManagers = x509TrustManagers;
		this.wantClientAuth = wantClientAuth;
		this.needClientAuth = needClientAuth;
		SecureRandom random = new SecureRandom();
		BcTlsCrypto crypto = new BcTlsCrypto(random);
		io = ioi;
		tlsInput = ByteBuffer.allocate(2048);
		tlsInput.order(order);

		this.serverProtocol = new TlsServerProtocol();
		var credentials = new SimpleCredentialsProvider(crypto, certificateContainer, hostname);
		this.server = new DefaultTls13Server(crypto, needClientAuth, wantClientAuth, getAcceptedIssuers(), credentials,
											 (clientCertificate, tlsUnique, tlsExporter) -> {
												 BcTLSIO.this.peerCertificate = clientCertificate;
												 BcTLSIO.this.tlsUnique = tlsUnique;
												 BcTLSIO.this.tlsExporter = tlsExporter;
												 BcTLSIO.this.handshakeCompleted = true;
												 eventHandler.handshakeCompleted(fakeWrapper);
											 });

		serverProtocol.accept(server);

		//pumpData();

		if (log.isLoggable(Level.FINER)) {
			log.log(Level.FINER, "TLS Socket created: {0}", io.toString());
		}
	}

	@Override
	public int bytesRead() {
		return bytesRead;
	}

	@Override
	public boolean checkCapabilities(String caps) {
		return caps.contains(TLS_CAPS) || io.checkCapabilities(caps);
	}

	@Override
	public long getBuffOverflow(boolean reset) {
		return io.getBuffOverflow(reset);
	}

	@Override
	public long getBytesReceived(boolean reset) {
		return io.getBytesReceived(reset);
	}

	@Override
	public long getBytesSent(boolean reset) {
		return io.getBytesSent(reset);
	}

	@Override
	public int getInputPacketSize() throws IOException {
		return io.getInputPacketSize();
	}

	@Override
	public SocketChannel getSocketChannel() {
		return io.getSocketChannel();
	}

	@Override
	public void getStatistics(StatisticsList list, boolean reset) {
		if (io != null) {
			io.getStatistics(list, reset);
		}
	}

	@Override
	public long getTotalBuffOverflow() {
		return io.getTotalBuffOverflow();
	}

	@Override
	public long getTotalBytesReceived() {
		return io.getTotalBytesReceived();
	}

	@Override
	public long getTotalBytesSent() {
		return io.getTotalBytesSent();
	}

	@Override
	public boolean isConnected() {
		return io.isConnected();
	}

	@Override
	public boolean isRemoteAddress(String addr) {
		return io.isRemoteAddress(addr);
	}

	@Override
	public void processHandshake(byte[] data) throws IOException {
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Process handshake data: " + data.length + " bytes.");
		}
//		System.out.println("C->S: wrapped bytes: " + Hex.toHexString(data));
		serverProtocol.offerInput(data);
		pumpData();
	}

	@Override
	public ByteBuffer read(ByteBuffer buff) throws IOException {
		pumpData();
		bytesRead = serverProtocol.readInput(buff.array(), buff.position(), buff.remaining());
		if (bytesRead > 0) {
			buff.position(buff.position() + bytesRead);
			buff.flip();
		}
		pumpData();

		return buff;
	}

	@Override
	public void setLogId(String logId) {
		io.setLogId(logId);
	}

	@Override
	public void stop() throws IOException {
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Stop called..." + this);

			// Thread.dumpStack();
		}

		io.stop();
		serverProtocol.close();
	}

	@Override
	public String toString() {
		return "TLS: " + io.toString();
	}

	@Override
	public boolean waitingToSend() {
		return io.waitingToSend();
	}

	@Override
	public int waitingToSendSize() {
		return io.waitingToSendSize();
	}

	@Override
	public int write(ByteBuffer buff) throws IOException {
		int result;
		try {

			pumpData();

			if (buff == null) {
				return io.write(null);
			}

			serverProtocol.writeApplicationData(buff.array(), buff.position(), buff.remaining());
			result = buff.remaining();
			buff.position(buff.position() + result);
			serverProtocol.flush();

			pumpData();

		} catch (IOException e) {
			log.log(Level.WARNING, "Cannot write data!", e);
			throw new SSLException(e);
		}

		return result;
	}

	private X509Certificate[] gen(Certificate chain) {
		if (chain == null) {
			return null;
		}
		try {
			X509Certificate[] result = new X509Certificate[chain.getLength()];

			for (int i = 0; i < chain.getLength(); i++) {
				TlsCertificate c = chain.getCertificateAt(i);
				X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509")
						.generateCertificate(new ByteArrayInputStream(c.getEncoded()));
				result[i] = cert;
			}
			return result;
		} catch (Exception e) {
			log.log(Level.WARNING, "Cannot create certificate", e);
			return null;
		}
	}

	private Collection<X500Name> getAcceptedIssuers() {
		if (clientTrustManagers != null) {
			ArrayList<X500Name> result = new ArrayList<>();

			for (TrustManager clientTrustManager : clientTrustManagers) {
				if (clientTrustManager instanceof X509TrustManager) {
					X509Certificate[] iss = ((X509TrustManager) clientTrustManager).getAcceptedIssuers();
					for (X509Certificate certificate : iss) {
						X500Name n = new X500Name(certificate.getSubjectX500Principal().toString());
						result.add(n);
					}
				}
			}

			return result;
		}
		return null;
	}

	private byte[] getBytes(final ByteBuffer buff) {
		byte[] tmp;
		if (buff != null) {
//			buff.flip();
			tmp = new byte[buff.remaining()];
			buff.get(tmp);
			buff.compact();
//			buff.flip();
		} else {
			tmp = null;
		}
		return tmp;
	}

	private void pumpData() throws IOException {
		int counter = 0;
		int resOut;
		int resIn;

		if (log.isLoggable(Level.FINEST)) {
			log.finest("Copying data from&to TLS Engine");
		}

		try {
			do {
				++counter;
				resOut = 0;
				// copy outgoing data (S->C)
				int waiting = serverProtocol.getAvailableOutputBytes();
				if (waiting > 0) {
					ByteBuffer bb = ByteBuffer.allocate(waiting);
					int dataLen = serverProtocol.readOutput(bb.array(), 0, bb.array().length);
					if (dataLen > 0) {
						resOut += dataLen;
//						System.out.println("S->C: " + resOut + " wrapped bytes: " + Hex.toHexString(bb.array()));
						bb.position(resOut);
						bb.flip();
						io.write(bb);
					}
				}

				// copy received data (C->S)
				resIn = 0;
				ByteBuffer bb = io.read(tlsInput);

				if (io.bytesRead() > 0) {
					byte[] tmp = getBytes(bb);
					if (tmp != null && tmp.length > 0) {
						resIn += tmp.length;
//						System.out.println("C->S: " + resIn + " wrapped bytes: " + Hex.toHexString(tmp));
						serverProtocol.offerInput(tmp);

					}
				}
			} while ((resIn > 0 || resOut > 0) && counter <= 1000);
		} catch (IOException e) {
			log.log(Level.WARNING, "Error on reading/writing data.", e);
			throw e;
		} catch (Throwable e) {
			log.log(Level.WARNING, "Error on reading/writing data.", e);
			throw new IOException("Data copying exception", e);
		}
	}

}