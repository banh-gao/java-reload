package com.github.reload.net;

import java.net.Socket;
import java.util.BitSet;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import com.github.reload.Context;
import com.github.reload.message.NodeID;
import com.github.reload.net.data.FramedMessage;
import com.github.reload.net.data.FramedMessage.FramedAck;
import com.github.reload.net.data.FramedMessage.FramedData;
import com.github.reload.net.ice.IceCandidate.OverlayLinkType;

/**
 * Factory to create neighbors objects specialized for specific link layers
 * types
 * 
 */
public class NodeFactory {

	private final Context context;

	public NodeFactory(Context context) {
		this.context = context;
	}

	/**
	 * Create a specialized neighbor node implementation for the given params
	 * 
	 * @param nodeId
	 *            the neighbor node-id, if null this neighbor is considered a to
	 *            be used only for bootstrap, in this case instead of returning
	 *            a null node-id, it will be used the first one in the
	 *            certificate.
	 * @return the neighbor node implementation adeguate to the specified link
	 *         layer type
	 * @throws CertificateException
	 */
	public NeighborNode newNeighborNode(Socket socket, NodeID nodeId, ReloadCertificate reloadCert, OverlayLinkType linkType) {
		switch (linkType) {
			case TLS_TCP_FH_NO_ICE :
				return new AckedNode(socket, nodeId, reloadCert, linkType, context);
			default :
				throw new UnsupportedOperationException("Unsupported " + linkType + " link type");
		}

	}
}

/**
 * Simple implementation with acked messages but no retransmission, an unacked
 * messages will be immediately reported to the sender without retransmission
 * attempts. The link delay time is static, this implementation it ignores the
 * link state reported by acks.
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
class AckedNode extends NeighborNode {

	private static final int LAST_RCV_PACKETS_BUF_SIZE = 32;

	// Initial link timeout in milliseconds (see RFC6298 Appendix A)
	private static final long INITIAL_TIMEOUT = 1000;
	private static final long MAX_TIMEOUT = 10000;

	// Constants for timeout calculation (see RFC6298 Section 2)
	private static final double K = 4;
	private static final double ALPHA = 1 / 8;
	private static final double BETA = 1 / 4;
	// The smoothed round-trip time
	private double sRtt = -1;
	// The round-trip time variation
	private double rttVar = -1;
	// Link timeout in milliseconds, after this time the message will be
	// considerated
	// unacked
	private long linkTimeout = INITIAL_TIMEOUT;

	// Sender side vars
	private long lastAckedSeqNum;
	private long nextSeq = 0;

	// Receiver side vars
	private final Queue<Long> lastReceivedSeqNums;

	public AckedNode(Socket socket, NodeID nodeId, ReloadCertificate reloadCert, OverlayLinkType linkType, Context context) {
		super(socket, nodeId, reloadCert, linkType, context);
		lastReceivedSeqNums = new LinkedBlockingQueue<Long>(LAST_RCV_PACKETS_BUF_SIZE) {

			@Override
			public boolean add(Long e) {
				if (remainingCapacity() == 0) {
					remove();
				}
				return super.add(e);
			}
		};
	}

	@Override
	protected void onAckReceived(FramedAck ackFrame, long rtt) {
		lastAckedSeqNum = ackFrame.getSequence();
		synchronized (this) {
			notify();
		}
		recalculateLinkTimeout(rtt);
	}

	private void recalculateLinkTimeout(long rtt) {
		// First misuration
		if (sRtt == -1) {
			sRtt = rtt;
			rttVar = rtt / 2;
		} else {
			rttVar = (1 - BETA) * rttVar + BETA * Math.abs(sRtt - rtt);
			sRtt = (1 - ALPHA) * sRtt + ALPHA * rtt;
		}
		long newTimeout = (long) (sRtt + K * rttVar);
		if (newTimeout < 1000)
			linkTimeout = 1000;
		else if (newTimeout > MAX_TIMEOUT)
			linkTimeout = MAX_TIMEOUT;
		else
			linkTimeout = newTimeout;
	}

	@Override
	protected FramedData getFramedData(UnsignedByteBuffer data) {
		long seq = nextSeq;

		FramedData frame = new FramedData(seq, data);

		nextSeq = (nextSeq + 1) % FramedMessage.SEQ_MAX_VALUE;
		return frame;
	}

	@Override
	protected FramedAck onDataReceived(FramedData data) {
		int receivedMask = computeReceivedMask(data.getSequence());
		lastReceivedSeqNums.add(data.getSequence());
		return new FramedAck(data.getSequence(), receivedMask);
	}

	private int computeReceivedMask(long lastSeqNum) {
		BitSet rcvBitSet = new BitSet(EncUtils.U_INT32);

		long u_lastSeqNum = lastSeqNum;
		u_lastSeqNum &= 0xff;
		for (long rcvSeq : lastReceivedSeqNums) {
			long u_rcvSeq = rcvSeq;
			u_rcvSeq &= 0xff;
			if (u_rcvSeq < u_lastSeqNum && u_rcvSeq > u_lastSeqNum - LAST_RCV_PACKETS_BUF_SIZE) {
				rcvBitSet.set((int) (u_lastSeqNum - u_rcvSeq));
			}
		}

		int rcv = 0;
		for (int i = 0; i < 32; i++)
			if (rcvBitSet.get(i)) {
				rcv |= (1 << i);
			}
		return rcv;

	}

	@Override
	protected long getLinkTimeout() {
		return linkTimeout;
	}

	@Override
	protected boolean isAcked(long seqNum) {
		return lastAckedSeqNum == seqNum;
	}
}