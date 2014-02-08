package com.github.reload.net.data;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import com.github.reload.net.data.Codec.Field;

/**
 * RELOAD link layer message
 */
public abstract class FramedMessage {

	public static enum FrameType {
		DATA(128), ACK(129);

		public final int code;

		FrameType(int code) {
			this.code = code;
		}

		public static FrameType valueOf(int code) {
			for (FrameType t : FrameType.values())
				if (t.code == code)
					return t;
			return null;
		}
	};

	public abstract long getSequence();

	public abstract FrameType getType();

	protected abstract void implEncode(ByteBuf buf);

	public static FramedMessage decode(ByteBuf in) throws DecoderException {
		FrameType type = FrameType.valueOf(in.readUnsignedByte());
		if (type == null)
			throw new DecoderException("Unknown frame type");

		switch (type) {
			case DATA :
				return FramedData.decode(in);
			case ACK :
				return FramedAck.decode(in);
		}

		// Should't happen: Unhandled message type
		assert false;

		return null;
	}

	public final void encode(ByteBuf buf) {
		implEncode(buf);
	}

	/**
	 * Link level data message
	 */
	public static class FramedData extends FramedMessage {

		private static final int DATA_MAX_LENGTH = Codec.U_INT24;

		protected long sequence;
		protected ByteBuf data;

		public FramedData(long sequence, ByteBuf data) {
			this.sequence = sequence;
			this.data = data;
		}

		public static FramedData decode(ByteBuf in) {
			return new FramedData(in.readUnsignedInt(), in.slice());
		}

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
		public void implEncode(ByteBuf buf) {
			buf.writeInt((int) sequence);
			Field dataFld = Codec.allocateField(buf, DATA_MAX_LENGTH);
			buf.writeBytes(data);
			dataFld.updateDataLength();
		}
	}

	/**
	 * Link level acknowledge message
	 */
	public static class FramedAck extends FramedMessage {

		protected long ack_sequence;
		protected int receivedBitMask;

		public static FramedAck decode(ByteBuf in) {
			FramedAck a = new FramedAck();
			a.ack_sequence = in.readUnsignedInt();
			a.receivedBitMask = in.readInt();
			return a;
		}

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
		public void implEncode(ByteBuf buf) {
			buf.writeInt((int) ack_sequence);
			buf.writeInt(receivedBitMask);
		}
	}
}
