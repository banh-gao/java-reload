package com.github.reload.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.Channels;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.github.reload.Context;
import com.github.reload.ReloadOverlay;
import com.github.reload.crypto.ReloadCertificate;
import com.github.reload.message.NodeID;
import com.github.reload.net.data.FramedMessage;
import com.github.reload.net.data.FramedMessage.FramedAck;
import com.github.reload.net.data.FramedMessage.FramedData;
import com.github.reload.net.ice.IceCandidate.OverlayLinkType;

/**
 * Represent a directly connected neighbor node, used to send and receive
 * messages, implementations are responsible for the RELOAD link layer
 * 
 */
public abstract class NeighborNode {

	private static final int TX_BUFFER_SIZE = 65536;
	private static final Logger logger = Logger.getLogger(ReloadOverlay.class);

	private final Context context;
	private final Socket socket;
	private final ReloadCertificate neighborCert;
	private final OverlayLinkType linkType;
	private final NodeID nodeId;
	private final boolean isBootstrapLink;

	private ReadableByteChannel in;
	private WritableByteChannel out;

	private PeerStatus.Updater statusUpdater;

	private final IncomingDataListener incomingListener;

	private final UnsignedByteBuffer txMessageBuf = UnsignedByteBuffer.allocate(TX_BUFFER_SIZE);

	private Map<Long, Long> startTimes = new ConcurrentHashMap<Long, Long>();

	/**
	 * @return the data frame for the passed message
	 */
	protected abstract FramedData getFramedData(UnsignedByteBuffer data);

	/**
	 * @return the acknowledgment frame to send in response to the specified
	 *         data frame, null if the data is unacked
	 */
	protected abstract FramedAck onDataReceived(FramedData data);

	/**
	 * @return true if the frame with the specified was acked, false otherwise
	 */
	protected abstract boolean isAcked(long seqNum);

	/**
	 * @return the link timeout in milliseconds before a message should me
	 *         considerated unacked, this value can be calculated dynamically by
	 *         using the RoundTripTime value passed when an ACK for a message is
	 *         received.
	 * @see #onAckReceived(FramedAck, long)
	 */
	protected long getLinkTimeout() {
		return 0;
	}

	/**
	 * @return The Maximum Transmission Unit this link can carry measured in
	 *         bytes. If an outgoing message exceedes this limit, the message
	 *         will be automatically divided into smaller RELOAD fragments.
	 */
	protected int getLinkMTU() {
		return TX_BUFFER_SIZE;
	}

	/**
	 * Handle an incoming acknowledgement frame, includes the RoundTripTime for
	 * the message over the link in milliseconds
	 */
	protected abstract void onAckReceived(FramedAck ackFrame, long rtt);

	protected NeighborNode(Socket socket, NodeID nodeId, ReloadCertificate neighborCert, OverlayLinkType linkType, Context context) {
		this.context = context;
		this.socket = socket;
		this.neighborCert = neighborCert;
		this.linkType = linkType;

		if (nodeId == null) {
			isBootstrapLink = true;
			this.nodeId = neighborCert.getNodeIds().iterator().next();
		} else {
			isBootstrapLink = false;
			this.nodeId = nodeId;
		}

		statusUpdater = context.getPeerStatusUpdater();

		incomingListener = new IncomingDataListener(context.getMessageHandler());

		try {
			this.in = Channels.newChannel(socket.getInputStream());
			this.out = Channels.newChannel(socket.getOutputStream());
		} catch (IOException e) {
			disconnect(e.getMessage());
			return;
		}

		incomingListener.setDaemon(true);
		incomingListener.setName("Listener " + context.getLocalId() + " << " + this.nodeId);
		incomingListener.start();

		logger.log(Priority.DEBUG, "Start listening to " + toString());
	}

	/**
	 * @return the nodeid this node is responsible for
	 */
	public NodeID getNodeId() {
		return nodeId;
	}

	/**
	 * @return True if the neighbor connection was established only for
	 *         bootstrap
	 */
	public boolean isBootstrapLink() {
		return isBootstrapLink;
	}

	public OverlayLinkType getLinkType() {
		return linkType;
	}

	/**
	 * @return the certificate associated with this neighbor
	 */
	public ReloadCertificate getCertificate() {
		return neighborCert;
	}

	private final void sendAckFor(FramedData dataFrame) {
		FramedAck ack = onDataReceived(dataFrame);
		if (ack == null)
			return;
		try {
			synchronized (out) {
				ack.writeTo(out);
			}
		} catch (SocketException e) {
			return;
		} catch (IOException e) {
			logger.log(Priority.DEBUG, e.getMessage(), e);
		}
	}

	/**
	 * Send the specified message to this node
	 * 
	 * @throws IOException
	 *             if the sending fails
	 */
	public void sendMessage(Message message, Context context) throws IOException {
		if (socket.isInputShutdown())
			throw new IOException("Node disconnected");

		txMessageBuf.clear();

		Field msgLenFld = message.writeHeader(txMessageBuf);
		UnsignedByteBuffer payloadBuf = txMessageBuf.slice();
		message.writePayloadTo(payloadBuf, context);
		txMessageBuf.position(txMessageBuf.position() + payloadBuf.position());

		int messageSize = txMessageBuf.getConsumedFrom(0);

		// Write total message size in the header length field
		msgLenFld.setEncodedLength(messageSize);

		int maxMessageSize = Math.min(getLinkMTU(), context.getConfiguration().getMaxMessageSize());

		if (messageSize > maxMessageSize) {
			payloadBuf.flip();
			sendMessageFragments(message.getHeader(), payloadBuf, messageSize, maxMessageSize);
			return;
		}

		txMessageBuf.flip();

		FramedData frame = getFramedData(txMessageBuf);

		startTimes.put(frame.getSequence(), System.currentTimeMillis());

		synchronized (out) {
			HandlingThread.releaseRunPermission();
			frame.writeTo(out);
			try {
				HandlingThread.acquireRunPermission();
			} catch (InterruptedException e) {
				throw new IOException(e);
			}
		}

		try {
			synchronized (this) {
				wait(getLinkTimeout());
			}

			if (!isAcked(frame.getSequence()))
				throw new IOException("Unacked data frame");

		} catch (InterruptedException e) {
			logger.log(Priority.FATAL, e.getMessage(), e);
		} finally {
			startTimes.remove(frame.getSequence());
		}

		statusUpdater.addSentBytes(frame.getLength());
	}

	private void sendMessageFragments(ForwardingHeader header, UnsignedByteBuffer payload, int messageSize, int maxMessageSize) throws IOException {
		// Leave enough space for via-list and dest-list to grow
		int fragmentPayloadSize = maxMessageSize - header.getHeaderLength() - 32;
		int neededFragments = (int) Math.ceil((double) messageSize / maxMessageSize);

		UnsignedByteBuffer payloadChunks = payload.clone();

		for (int i = 0; i < neededFragments; i++) {
			int limit = Math.min((i + 1) * fragmentPayloadSize, payload.limit());
			payloadChunks.limit(limit);
			int payloadOffset = header.getFragmentOffset() + i * fragmentPayloadSize;
			boolean isLast = (header.isLastFragment() && i == neededFragments - 1);
			ForwardingHeader fragHeader = ForwardingHeader.getFragmentHeader(header, payloadOffset, isLast);
			System.out.println(fragHeader);
			sendMessage(new EncodedMessage(fragHeader, payloadChunks), context);
		}
		System.exit(1);
	}

	public final void disconnect(String reason) {
		if (incomingListener == null || incomingListener.isInterrupted())
			return;

		incomingListener.interrupt();
		try {
			socket.close();
			incomingListener.join();
		} catch (Exception e) {
			// Ignored
		}

		context.getTopologyPlugin().onNeighborDisconnected(this);

		logger.log(Priority.DEBUG, "Connection with " + this + " closed: " + reason);
	}

	@Override
	public int hashCode() {
		return nodeId.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NeighborNode other = (NeighborNode) obj;

		return nodeId.equals(other.nodeId);
	}

	@Override
	public String toString() {
		if (isBootstrapLink)
			return "Bootstrap link at " + getRemoteSocketAddress();
		else
			return "Neighbor " + nodeId;
	}

	/**
	 * Listen for incoming messages and handle with messagehandler
	 * 
	 */
	private class IncomingDataListener extends Thread {

		private final IncomingMessageHandler messageHandler;

		public IncomingDataListener(IncomingMessageHandler messageHandler) {
			this.messageHandler = messageHandler;
		}

		@Override
		public void run() {
			while (!isInterrupted()) {
				try {
					FramedMessage frame = FramedMessage.parse(in);
					handleIncomingFrame(frame);
				} catch (ClosedByInterruptException e) {
					// Listener interrupted
				} catch (Exception e) {
					logger.log(Priority.DEBUG, e.getMessage(), e);
					disconnect(e.getMessage());
					interrupt();
				}
			}
		}

		private void handleIncomingFrame(FramedMessage frame) {
			if (frame instanceof FramedData) {
				sendAckFor((FramedData) frame);
				messageHandler.handleMessage(NeighborNode.this, ((FramedData) frame).getData());
			} else if (frame instanceof FramedAck) {
				long txStartTime = startTimes.get(frame.getSequence());
				onAckReceived((FramedAck) frame, txStartTime - System.currentTimeMillis());
			} else {
				logger.log(Priority.FATAL, "Unknown frame type " + frame.getClass().getCanonicalName());
			}
			statusUpdater.addReceivedBytes(frame.getLength());
		}
	}

	public boolean isConnected() {
		return socket.isConnected();
	}

	public InetSocketAddress getRemoteSocketAddress() {
		return (InetSocketAddress) socket.getRemoteSocketAddress();
	}
}
