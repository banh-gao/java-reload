package com.github.reload.storage.errors;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import java.util.ArrayList;
import java.util.List;
import com.github.reload.message.errors.Error;
import com.github.reload.message.errors.ErrorRespose;
import com.github.reload.message.errors.ErrorType;
import com.github.reload.net.data.Codec;
import com.github.reload.net.data.Codec.Field;
import com.github.reload.storage.KindId;

/**
 * Indicates that some kinds are unknown and report them in the error
 * information
 * 
 */
public class UnknownKindException extends Exception implements ErrorRespose {

	private static final int KINDS_LENGTH_FIELD = Codec.U_INT8;

	private final List<KindId> unknownKinds;

	public UnknownKindException(List<KindId> kindIds) {
		super(getEncodedKinds(kindIds));
		unknownKinds = kindIds;
	}

	public UnknownKindException(String info) {
		super("Unknown data kinds");

		byte[] infoData = info.getBytes(Error.MSG_CHARSET);

		ByteBuf buf = UnpooledByteBufAllocator.DEFAULT.buffer(infoData.length);
		buf.writeBytes(infoData);
		ByteBuf unknownKindsData = Codec.readField(buf, KINDS_LENGTH_FIELD);

		unknownKinds = new ArrayList<KindId>();

		while (unknownKindsData.readableBytes() > 0) {
			KindId kindId = KindId.valueOf(buf.readInt());
			unknownKinds.add(kindId);
		}

		unknownKindsData.release();
	}

	private static String getEncodedKinds(List<KindId> kindIds) {
		ByteBuf buf = UnpooledByteBufAllocator.DEFAULT.buffer();

		Field lenFld = Codec.allocateField(buf, KINDS_LENGTH_FIELD);

		for (KindId id : kindIds) {
			buf.writeInt((int) id.getId());
		}

		lenFld.updateDataLength();

		byte[] out = new byte[buf.readableBytes()];
		buf.readBytes(out);

		return new String(out, Error.MSG_CHARSET);
	}

	public List<KindId> getUnknownKinds() {
		return unknownKinds;
	}

	@Override
	public ErrorType getErrorType() {
		return ErrorType.UNKNOWN_KIND;
	}

	@Override
	public String getMessage() {
		return "Unknown data kinds: " + unknownKinds;
	}
}
