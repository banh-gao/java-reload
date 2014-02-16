package com.github.reload.message.content;

import io.netty.buffer.ByteBuf;
import com.github.reload.Context;
import com.github.reload.message.Content;
import com.github.reload.message.ContentType;
import com.github.reload.message.content.PingRequest.PingRequestCodec;
import com.github.reload.net.data.Codec;
import com.github.reload.net.data.ReloadCodec;

@ReloadCodec(PingRequestCodec.class)
public class PingRequest extends Content {

	private final int payloadLength;

	public PingRequest() {
		payloadLength = 0;
	}

	public PingRequest(int payloadLength) {
		this.payloadLength = payloadLength;
	}

	public int getPayloadLength() {
		return payloadLength;
	}

	@Override
	public ContentType getType() {
		return ContentType.PING_REQ;
	}

	public static class PingRequestCodec extends Codec<PingRequest> {

		private static final int PADDING_LENGTH_FIELD = U_INT16;

		public PingRequestCodec(Context context) {
			super(context);
		}

		@Override
		public void encode(PingRequest obj, ByteBuf buf) throws com.github.reload.net.data.Codec.CodecException {
			Field lenFld = allocateField(buf, PADDING_LENGTH_FIELD);
			buf.writerIndex(buf.writerIndex() + obj.payloadLength);
			lenFld.updateDataLength();
		}

		@Override
		public PingRequest decode(ByteBuf buf) throws com.github.reload.net.data.Codec.CodecException {
			ByteBuf padding = readField(buf, PADDING_LENGTH_FIELD);
			return new PingRequest(padding.readableBytes());
		}

	}

}
