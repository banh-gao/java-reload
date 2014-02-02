package com.github.reload.net.data;

import io.netty.buffer.ByteBuf;
import com.github.reload.net.data.CodecUtils.Field;

/**
 * RELOAD link layer message
 */
public abstract class FrameMessage implements Encodable {

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

		private static final int DATA_MAX_LENGTH = CodecUtils.U_INT24;

		protected long sequence;
		protected ByteBuf data;

		@Override
		public FrameType getType() {
			return FrameType.DATA;
		}

		@Override
		public long getSequence() {
			return sequence;
		}

		public ByteBuf getData() {
			return data;
		}

		@Override
		public void encode(ByteBuf buf) {
			buf.writeInt((int) sequence);
			Field dataFld = CodecUtils.allocateField(buf, DATA_MAX_LENGTH);
			buf.writeBytes(data);
			dataFld.updateDataLength();
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

		@Override
		public void encode(ByteBuf buf) {
			buf.writeInt((int) ack_sequence);
			buf.writeInt(receivedBitMask);
		}

	}
}
