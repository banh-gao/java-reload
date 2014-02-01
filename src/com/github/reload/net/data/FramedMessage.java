package com.github.reload.net.data;

import io.netty.buffer.ByteBuf;

/**
 * RELOAD link layer message
 */
abstract class FramedMessage {

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

	public abstract FrameType getType();

	public static class FramedData extends FramedMessage {

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

	}

	public static class FramedAck extends FramedMessage {

		@Override
		public FrameType getType() {
			return FrameType.ACK;
		}

	}
}
