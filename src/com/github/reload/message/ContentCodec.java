package com.github.reload.message;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.List;
import com.github.reload.Context;
import com.github.reload.net.data.Codec;

public class ContentCodec extends Codec<Content> {

	private static final int BODY_LENGTH_FIELD = U_INT32;
	private static final int EXTENSIONS_LENGTH_FIELD = U_INT32;

	public ContentCodec(Context context) {
		super(context);
	}

	@Override
	public void encode(Content obj, ByteBuf buf, Object... params) throws com.github.reload.net.data.Codec.CodecException {
		buf.writeShort(obj.getType().getCode());

		Field lenFld = allocateField(buf, BODY_LENGTH_FIELD);

		@SuppressWarnings("unchecked")
		Codec<Content> codec = (Codec<Content>) getCodec(obj.getClass());

		codec.encode(obj, buf);

		lenFld.updateDataLength();

		encodeExtensions(obj, buf);
	}

	private void encodeExtensions(Content obj, ByteBuf buf) throws com.github.reload.net.data.Codec.CodecException {
		Field lenFld = allocateField(buf, EXTENSIONS_LENGTH_FIELD);

		for (MessageExtension ex : obj.getMessageExtensions()) {
			@SuppressWarnings("unchecked")
			Codec<MessageExtension> codec = (Codec<MessageExtension>) getCodec(ex.getClass());
			codec.encode(ex, buf);
		}

		lenFld.updateDataLength();
	}

	@Override
	public Content decode(ByteBuf buf, Object... params) throws com.github.reload.net.data.Codec.CodecException {

		short messageCode = buf.readShort();

		ContentType contentType = ContentType.valueOf(messageCode);

		if (contentType == null)
			throw new DecoderException("Unsupported message with code " + messageCode);

		Codec<? extends Content> codec = getCodec(contentType.getContentClass());

		ByteBuf contentData = readField(buf, BODY_LENGTH_FIELD);
		Content content = codec.decode(contentData);

		content.messageExtensions = decodeExtensions(buf);
		return content;
	}

	private List<MessageExtension> decodeExtensions(ByteBuf buf) throws com.github.reload.net.data.Codec.CodecException {
		List<MessageExtension> out = new ArrayList<MessageExtension>();

		ByteBuf extData = readField(buf, EXTENSIONS_LENGTH_FIELD);

		Codec<MessageExtension> codec = getCodec(MessageExtension.class);

		while (extData.readableBytes() > 0) {
			out.add(codec.decode(extData));
		}

		extData.release();

		return out;
	}
}