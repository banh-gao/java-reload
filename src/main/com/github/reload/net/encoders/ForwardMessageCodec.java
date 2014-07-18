package com.github.reload.net.encoders;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import java.util.List;
import org.apache.log4j.Logger;
import com.github.reload.Configuration;
import com.github.reload.net.encoders.header.Header;

/**
 * Codec used to process only the Forwarding Header part of the message
 */
public class ForwardMessageCodec extends ByteToMessageCodec<ForwardMessage> {

	/**
	 * Size in bytes of the first part of the header from the beginning to the
	 * message length field
	 */
	public static int HDR_LEADING_LEN = 16;

	private final Codec<Header> hdrCodec;

	public ForwardMessageCodec(Configuration conf) {
		hdrCodec = Codec.getCodec(Header.class, conf);
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		try {
			ForwardMessage message = new ForwardMessage();
			message.header = hdrCodec.decode(in);
			message.payload = in.slice();
			in.retain();
			out.add(message);
			Logger.getRootLogger().debug(String.format("Message header %#x decoded", message.header.getTransactionId()));
		} finally {
			in.clear();
		}
	}

	@Override
	protected void encode(ChannelHandlerContext ctx, ForwardMessage msg, ByteBuf out) throws Exception {
		int messageStart = out.writerIndex();

		hdrCodec.encode(msg.header, out);
		Logger.getRootLogger().debug(String.format("Message header %#x encoded", msg.header.getTransactionId()));

		out.writeBytes(msg.payload);

		setMessageLength(out, messageStart);
	}

	private void setMessageLength(ByteBuf buf, int messageStart) {
		int messageLength = buf.writerIndex() - messageStart;
		buf.setInt(messageStart + HDR_LEADING_LEN, messageLength);
	}
}
