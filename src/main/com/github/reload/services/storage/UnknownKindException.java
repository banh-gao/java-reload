package com.github.reload.services.storage;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import java.util.ArrayList;
import java.util.List;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.Field;
import com.github.reload.net.encoders.content.Error;
import com.github.reload.net.encoders.content.Error.ErrorMessageException;
import com.github.reload.net.encoders.content.Error.ErrorType;

/**
 * Indicates that some kinds are unknown and report them in the error
 * information
 * 
 */
public class UnknownKindException extends ErrorMessageException {

	private static final int KINDS_LENGTH_FIELD = Codec.U_INT8;

	private final List<Long> unknownKinds;

	public UnknownKindException(List<Long> kindIds) {
		super(new Error(ErrorType.UNKNOWN_KIND, getEncodedKinds(kindIds)));
		unknownKinds = kindIds;
	}

	public UnknownKindException(String info) {
		super(new Error(ErrorType.UNKNOWN_KIND, info.getBytes(Error.MSG_CHARSET)));

		byte[] infoData = getInfo();

		ByteBuf buf = UnpooledByteBufAllocator.DEFAULT.buffer(infoData.length);
		buf.writeBytes(infoData);
		ByteBuf unknownKindsData = Codec.readField(buf, KINDS_LENGTH_FIELD);

		unknownKinds = new ArrayList<Long>();

		while (unknownKindsData.readableBytes() > 0) {
			unknownKinds.add(buf.readUnsignedInt());
		}

		unknownKindsData.release();
	}

	private static byte[] getEncodedKinds(List<Long> kindIds) {
		ByteBuf buf = UnpooledByteBufAllocator.DEFAULT.buffer();

		Field lenFld = Codec.allocateField(buf, KINDS_LENGTH_FIELD);

		for (Long id : kindIds) {
			buf.writeInt(id.intValue());
		}

		lenFld.updateDataLength();

		byte[] out = new byte[buf.readableBytes()];
		buf.readBytes(out);

		buf.release();

		return out;
	}

	public List<Long> getUnknownKinds() {
		return unknownKinds;
	}

	@Override
	public String getMessage() {
		return "Unknown data kinds: " + unknownKinds;
	}
}
