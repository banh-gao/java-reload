package com.github.reload.net.encoders;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CodecException;
import java.util.List;
import org.apache.log4j.Logger;
import com.github.reload.components.ComponentsContext;
import com.github.reload.net.connections.Connection;
import com.github.reload.net.encoders.header.NodeID;

/**
 * Codec used to process only the Forwarding Header part of the message
 */
public class MessageHeaderDecoder extends ByteToMessageDecoder {

	private final Codec<Header> hdrCodec;

	private NodeID neighborId;

	public MessageHeaderDecoder(ComponentsContext ctx) {
		hdrCodec = Codec.getCodec(Header.class, ctx);
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		super.handlerAdded(ctx);
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		if (neighborId == null) {
			neighborId = ctx.channel().attr(Connection.CONNECTION).get().getNodeId();
		}

		try {

			ForwardMessage message = new ForwardMessage();

			message.header = hdrCodec.decode(in);

			message.header.setAttribute(Header.PREV_HOP, neighborId);

			if (in.readableBytes() != message.header.getPayloadLength())
				throw new CodecException("Payload length not matching with length specified in header");

			message.payload = in.slice();
			in.retain();
			out.add(message);
			Logger.getRootLogger().trace(String.format("Message header %#x decoded", message.header.getTransactionId()));
		} finally {
			in.clear();
		}
	}

	@Override
	protected void decodeLast(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		// Ignore last decode since no data has to be on the buffer
		if (in.readableBytes() > 0)
			throw new IllegalStateException("Unprocessed input bytes");
	}

}
