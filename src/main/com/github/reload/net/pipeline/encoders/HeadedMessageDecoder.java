package com.github.reload.net.pipeline.encoders;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;
import org.apache.log4j.Logger;
import com.github.reload.Configuration;
import com.github.reload.message.Codec;
import com.github.reload.message.Header;

/**
 * Decoder used to process only the Forwarding Header part of the message
 */
public class HeadedMessageDecoder extends ByteToMessageDecoder {

	private final Codec<Header> hdrCodec;

	public HeadedMessageDecoder(Configuration conf) {
		hdrCodec = Codec.getCodec(Header.class, conf);
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		try {
			HeadedMessage message = new HeadedMessage();
			message.header = hdrCodec.decode(in);
			message.payload = in.slice();
			in.retain();
			out.add(message);
			Logger.getRootLogger().debug("Message header #" + message.header.getTransactionId() + " decoded");
		} finally {
			in.clear();
		}
	}

	@Override
	protected void decodeLast(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		// Ignore
	}
}
