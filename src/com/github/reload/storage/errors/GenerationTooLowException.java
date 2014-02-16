package com.github.reload.storage.errors;

import java.math.BigInteger;
import java.util.List;
import net.sf.jReload.Context;
import net.sf.jReload.message.EncUtils;
import net.sf.jReload.message.UnsignedByteBuffer;
import net.sf.jReload.overlay.errors.Error;
import net.sf.jReload.overlay.errors.Error.ErrorType;
import net.sf.jReload.storage.StoreAnswer;
import net.sf.jReload.storage.StoreResponse;

/**
 * Indicates that a store request has the generation value lower than the stored
 * generation value. Contains a store answer with the stored generation value.
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class GenerationTooLowException extends StorageException {

	private BigInteger generation;

	private final UnsignedByteBuffer info;

	public GenerationTooLowException(BigInteger generation) {
		super("Request generation counter lower than stored data counter");
		this.generation = generation;
		info = null;
	}

	public GenerationTooLowException(byte[] info) {
		super("Request generation counter lower than stored data counter");
		this.info = UnsignedByteBuffer.wrap(info);
	}

	public GenerationTooLowException(List<StoreResponse> tooLowResponses) {
		super("Request generation counter lower than stored data counter");
		StoreAnswer answer = new StoreAnswer(tooLowResponses);
		info = UnsignedByteBuffer.allocate((int) EncUtils.maxUnsignedInt(INFO_LENGTH_FIELD));
		answer.writeTo(info);
	}

	public BigInteger getGeneration() {
		return generation;
	}

	public StoreAnswer getAnswer(Context context) throws UnknownKindException {
		return new StoreAnswer(context, info);
	}

	@Override
	public Error getError() {
		byte[] data = new byte[info.position()];
		info.getRaw(data);
		return new Error(ErrorType.GEN_COUNTER_TOO_LOW, data);
	}
}
