package com.github.reload.storage.errors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.sf.jReload.message.EncUtils;
import net.sf.jReload.message.UnsignedByteBuffer;
import net.sf.jReload.message.UnsignedByteBuffer.Field;
import net.sf.jReload.overlay.errors.Error;
import net.sf.jReload.overlay.errors.Error.ErrorType;
import net.sf.jReload.storage.KindId;

/**
 * Indicates that some kinds are unknown and report them in the error
 * information
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class UnknownKindException extends StorageException {

	private static final int KINDS_LENGTH_FIELD = EncUtils.U_INT8;

	private final List<KindId> unknownKinds;

	public UnknownKindException(String message) {
		super(message);
		unknownKinds = new ArrayList<KindId>();
	}

	public UnknownKindException(KindId... kindIds) {
		super("Unknown kinds " + Arrays.toString(kindIds));
		unknownKinds = Arrays.asList(kindIds);
	}

	public UnknownKindException(byte[] info) {
		super("Unknown kinds");

		UnsignedByteBuffer buf = UnsignedByteBuffer.wrap(info);
		int length = buf.getLengthValue(UnknownKindException.KINDS_LENGTH_FIELD);

		unknownKinds = new ArrayList<KindId>();

		int startPos = buf.position();

		while (buf.getConsumedFrom(startPos) < length) {
			KindId kindId = KindId.valueOf(buf);
			unknownKinds.add(kindId);
		}
	}

	public List<KindId> getUnknownKinds() {
		return unknownKinds;
	}

	@Override
	public Error getError() {
		return new Error(ErrorType.UNKNOWN_KIND, getEncodedKinds());
	}

	private byte[] getEncodedKinds() {
		UnsignedByteBuffer buf = UnsignedByteBuffer.allocate(UnknownKindException.KINDS_LENGTH_FIELD + KindId.MAX_LENGTH);

		Field lenFld = buf.allocateLengthField(KINDS_LENGTH_FIELD);

		for (KindId id : unknownKinds) {
			id.writeTo(buf);
		}

		lenFld.setEncodedLength(buf.getConsumedFrom(lenFld.getNextPosition()));

		return buf.flip().slice().array();
	}
}
