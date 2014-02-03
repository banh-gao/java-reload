package com.github.reload.net.data;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import com.github.reload.net.data.CodecUtils.Field;

/**
 * RELOAD link layer message
 */
public abstract class FrameMessage implements Encodable {

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

	public static FrameMessage decode(ByteBuf in) throws DecoderException {
		FrameType type = FrameType.valueOf(in.readUnsignedByte());
		if (type == null)
			throw new DecoderException("Unknown frame type");

		switch (type) {
			case DATA :
				return FramedData.decode(in);
			case ACK :
				return FramedAck.decode(in);
		}
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public final void encode(ByteBuf buf) {
		// TODO Auto-generated method stub
		implEncode(buf);
	}

	public static class FramedData extends FrameMessage {

		private static final int DATA_MAX_LENGTH = CodecUtils.U_INT24;

		protected long sequence;
		protected ByteBuf data;

		public static FramedData decode(ByteBuf in) {
			FramedData d = new FramedData();
			d.sequence = in.readUnsignedInt();
			d.data = in.slice();
			return d;
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
			Field dataFld = CodecUtils.allocateField(buf, DATA_MAX_LENGTH);
			buf.writeBytes(data);
			dataFld.updateDataLength();
		}
	}

	public static class FramedAck extends FrameMessage {

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
