package com.github.reload.net.pipeline.encoders;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.handler.codec.DecoderException;
import java.util.List;
import org.apache.log4j.Logger;
import com.github.reload.message.Codec;
import com.github.reload.message.Codec.CodecException;
import com.github.reload.message.Codec.Field;
import com.github.reload.net.pipeline.encoders.FramedMessage.FrameType;
import com.github.reload.net.pipeline.encoders.FramedMessage.FramedAck;
import com.github.reload.net.pipeline.encoders.FramedMessage.FramedData;

/**
 * Codec for RELOAD frame messages exchanged on a link to a neighbor node
 */
public class FramedMessageCodec extends ByteToMessageCodec<FramedMessage> {

	private static final int DATA_MAX_LENGTH = Codec.U_INT24;

	private static final int ACK_FRAME_LENGTH = 8;

	@Override
	protected void encode(ChannelHandlerContext ctx, FramedMessage msg, ByteBuf out) throws Exception {
		out.writeByte(msg.getType().code);
		out.writeInt((int) msg.getSequence());

		switch (msg.getType()) {
			case DATA :
				encodeData((FramedData) msg, out);
				break;
			case ACK :
				encodeAck((FramedAck) msg, out);
				break;
		}
		Logger.getRootLogger().debug("Message frame #" + msg.getSequence() + " encoded");
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		try {
			if (!isEnoughtData(in))
				return;

			FrameType type = FrameType.valueOf(in.readUnsignedByte());

			if (type == null)
				throw new DecoderException("Unknown frame type");

			long sequence = in.readUnsignedInt();

			FramedMessage msg = null;

			switch (type) {
				case DATA :
					msg = decodeData(in, sequence);
					break;
				case ACK :
					msg = decodeAck(in, sequence);
					break;
			}
			out.add(msg);
			Logger.getRootLogger().debug("Message frame #" + msg.getSequence() + " decoded");
		} finally {
			in.clear();
		}
	}

	/**
	 * Determine if enought data has been arrived in order to decode an entire
	 * message
	 * 
	 * @param in
	 * @return
	 */
	private boolean isEnoughtData(ByteBuf in) {
		in = in.slice();

		// Read frame type
		if (in.readableBytes() < 1)
			return false;
		FrameType type = FrameType.valueOf(in.readUnsignedByte());

		if (type == null)
			throw new DecoderException("Unknown frame type");

		// Read frame sequence
		if (in.readableBytes() < 4)
			return false;
		in.readUnsignedInt();

		switch (type) {
			case DATA :
				if (in.readableBytes() < DATA_MAX_LENGTH)
					return false;

				if (in.readableBytes() < Codec.readLength(in, DATA_MAX_LENGTH))
					return false;

				break;
			case ACK :
				if (in.readableBytes() < ACK_FRAME_LENGTH)
					return false;
				break;
		}

		return true;
	}

	public FramedMessage decode(ByteBuf buf) throws CodecException {
		FrameType type = FrameType.valueOf(buf.readUnsignedByte());

		if (type == null)
			throw new DecoderException("Unknown frame type");

		long sequence = buf.readUnsignedInt();

		switch (type) {
			case DATA :
				return decodeData(buf, sequence);
			case ACK :
				return decodeAck(buf, sequence);
		}

		throw new UnsupportedOperationException("Framed message type " + type + " not handled");
	}

	public static FramedData decodeData(ByteBuf in, long sequence) {
		ByteBuf payload = Codec.readField(in, DATA_MAX_LENGTH);
		return new FramedData(sequence, payload);
	}

	public static FramedAck decodeAck(ByteBuf in, long sequence) {
		return new FramedAck(sequence, in.readInt());
	}

	public static void encodeData(FramedData msg, ByteBuf out) {
		Field dataFld = Codec.allocateField(out, DATA_MAX_LENGTH);
		out.writeBytes(msg.getPayload());
		dataFld.updateDataLength();
	}

	public static void encodeAck(FramedAck msg, ByteBuf out) {
		out.writeInt(msg.getReceivedMask());
	}
}
