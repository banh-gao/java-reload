package com.github.reload.net.data;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import com.github.reload.Configuration;
import com.github.reload.message.Content;
import com.github.reload.message.Header;
import com.github.reload.message.HeaderCodec;
import com.github.reload.message.SecurityBlock;

/**
 * Encode the message to be exchanged on the RELOAD overlay
 */
public class MessageEncoder extends MessageToByteEncoder<Message> {

	private static final int MAX_MESSAGE_SIZE = 5000;

	private final Codec<Header> hdrCodec;
	private final Codec<Content> contentCodec;
	private final Codec<SecurityBlock> secBlockCodec;

	public MessageEncoder(Configuration conf) {
		hdrCodec = Codec.getCodec(Header.class, conf);
		contentCodec = Codec.getCodec(Content.class, conf);
		secBlockCodec = Codec.getCodec(SecurityBlock.class, conf);
	}

	@Override
	protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws Exception {
		out.capacity(MAX_MESSAGE_SIZE);

		int messageStart = out.writerIndex();

		hdrCodec.encode(msg.header, out);
		contentCodec.encode(msg.content, out);
		secBlockCodec.encode(msg.secBlock, out);

		updateMessageLength(out, messageStart);
	}

	private void updateMessageLength(ByteBuf buf, int messageStart) {
		int messageLength = buf.writerIndex() - messageStart;
		buf.setInt(messageStart + HeaderCodec.HDR_LEADING_LEN, messageLength);
	}
}
