package com.github.reload.net.codecs;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.log4j.Logger;
import com.github.reload.net.MessageRouter;
import com.github.reload.net.codecs.Codec.CodecException;
import com.github.reload.net.codecs.content.Content;
import com.github.reload.net.codecs.content.Error;
import com.github.reload.net.codecs.content.Error.ErrorMessageException;
import com.github.reload.net.codecs.secBlock.SecurityBlock;

/**
 * Codec for message payload (content + security block)
 */
@Sharable
@Singleton
public class MessagePayloadDecoder extends MessageToMessageDecoder<ForwardMessage> {

	@Inject
	@Named("contentCodec")
	Codec<Content> contentCodec;

	@Inject
	@Named("secBlockCodec")
	Codec<SecurityBlock> secBlockCodec;

	@Inject
	MessageRouter router;

	@Inject
	public MessagePayloadDecoder() {
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ForwardMessage msg, List<Object> out) throws Exception {
		Header header = msg.getHeader();
		ByteBuf payload = msg.getPayload();
		try {
			int contentStart = payload.readerIndex();

			try {
				Content content = contentCodec.decode(payload);

				ByteBuf rawContent = payload.copy(contentStart, payload.readerIndex() - contentStart);

				SecurityBlock secBlock = secBlockCodec.decode(payload);

				Message outMsg = new Message(header, content, secBlock);

				header.setAttribute(Header.RAW_CONTENT, rawContent);

				out.add(outMsg);
				Logger.getRootLogger().trace(String.format("Message payload %#x decoded", header.getTransactionId()));
			} catch (CodecException e) {
				if (e instanceof ErrorMessageException) {
					ErrorMessageException error = (ErrorMessageException) e;
					Error content = new Error(error.getType(), error.getInfo());
					router.sendAnswer(header, content);
					Logger.getRootLogger().debug(String.format("Sent error message caused by decoding of %#x: %s", header.getTransactionId(), e.getMessage()));
				} else {
					Logger.getRootLogger().warn(String.format("Message payload %#x decoding failed: %s", header.getTransactionId(), e.getMessage()));
				}
			}
		} finally {
			payload.release();
		}
	}
}