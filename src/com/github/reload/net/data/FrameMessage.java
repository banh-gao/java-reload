package com.github.reload.net.data;

import io.netty.buffer.ByteBuf;

/**
 * RELOAD link layer message
 */
public abstract class FrameMessage {

	public enum FrameType {
		DATA(128), ACK(129);

		public final int code;

		FrameType(int code) {
			this.code = code;
		}

		public FrameType valueOf(int code) {
			for (FrameType t : FrameType.values())
				if (t.code == code)
					return t;
			return null;
		}
	};

	public abstract long getSequence();

	public abstract FrameType getType();

	public static class FramedData extends FrameMessage {

		private final long sequence;
		private final ByteBuf data;

		public FramedData(long sequence, ByteBuf data) {
			this.sequence = sequence;
			this.data = data;
		}

		@Override
		public FrameType getType() {
			return FrameType.DATA;
		}

		@Override
		public long getSequence() {
			return sequence;
		}

	}

	public static class FramedAck extends FrameMessage {

		protected long ack_sequence;
		protected int receivedBitMask;

		@Override
		public FrameType getType() {
			return FrameType.ACK;
		}

		@Override
		public long getSequence() {
			return ack_sequence;
		}

		public int getReceivedMask() {
			return receivedBitMask;
		}

	}
}
