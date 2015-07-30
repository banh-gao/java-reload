package com.github.reload.net.stack;

import io.netty.buffer.ByteBuf;
import java.util.BitSet;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.inject.Inject;
import com.github.reload.net.codecs.FramedMessage;
import com.github.reload.net.codecs.FramedMessage.FramedAck;
import com.github.reload.net.codecs.FramedMessage.FramedData;

/**
 * Simple Reliability implementation with acked messages but no retransmission,
 * an unacked messages will be immediately reported to the sender without
 * retransmission attempts. The link delay time is static, this implementation
 * it ignores the link state reported by acks.
 */
public class SRLinkHandler extends LinkHandler {

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
	// considerated unacked
	private long linkTimeout = INITIAL_TIMEOUT;

	// Next sequence number to use
	private long nextSeq = 0;

	// Receiver side vars
	private final Queue<Long> lastReceivedSeqNums;

	@Inject
	public SRLinkHandler() {
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
	protected FramedData getDataFrame(ByteBuf payload) {
		long seq = nextSeq;

		FramedData frame = new FramedData(seq, payload);

		nextSeq = (nextSeq + 1) % FramedMessage.SEQ_MAX_VALUE;
		return frame;
	}

	@Override
	protected void handleData(FramedData data) {
		int receivedMask = computeReceivedMask(data.getSequence());
		lastReceivedSeqNums.add(data.getSequence());
		sendAckFrame(new FramedAck(data.getSequence(), receivedMask));
	}

	private int computeReceivedMask(long lastSeqNum) {
		BitSet rcvBitSet = new BitSet(LAST_RCV_PACKETS_BUF_SIZE);

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
		for (int i = 0; i < LAST_RCV_PACKETS_BUF_SIZE; i++)
			if (rcvBitSet.get(i)) {
				rcv |= (1 << i);
			}
		return rcv;

	}

	@Override
	protected void handleAck(FramedAck ack, Transmission request) {
		recalculateLinkTimeout(request.getRTT());
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
		if (newTimeout < 1000) {
			linkTimeout = 1000;
		} else if (newTimeout > MAX_TIMEOUT) {
			linkTimeout = MAX_TIMEOUT;
		} else {
			linkTimeout = newTimeout;
		}
	}

	@Override
	protected long getLinkTimeout() {
		return linkTimeout;
	}
}
