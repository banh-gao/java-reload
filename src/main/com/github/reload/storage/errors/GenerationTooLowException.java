package com.github.reload.storage.errors;

import java.math.BigInteger;
import com.github.reload.net.encoders.content.errors.Error;
import com.github.reload.net.encoders.content.errors.ErrorRespose;
import com.github.reload.net.encoders.content.errors.ErrorType;

/**
 * Indicates that a store request has the generation value lower than the stored
 * generation value. Contains a store answer with the stored generation value.
 * 
 */
public class GenerationTooLowException extends Exception implements ErrorRespose {

	private final BigInteger generation;

	public GenerationTooLowException(String info) {
		generation = new BigInteger(1, info.getBytes(Error.MSG_CHARSET));
	}

	public GenerationTooLowException(BigInteger generation) {
		super("Request generation counter lower than stored data counter");
		this.generation = generation;
	}

	public BigInteger getGeneration() {
		return generation;
	}

	@Override
	public ErrorType getErrorType() {
		return ErrorType.GEN_COUNTER_TOO_LOW;
	}
}
