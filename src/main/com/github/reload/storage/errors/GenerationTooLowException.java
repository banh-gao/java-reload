package com.github.reload.storage.errors;

import com.github.reload.net.encoders.content.errors.ErrorRespose;
import com.github.reload.net.encoders.content.errors.ErrorType;
import com.github.reload.net.encoders.content.storage.StoreAnswer;

/**
 * Indicates that a store request has the generation value lower than the stored
 * generation value. Contains a store answer with the stored generation value.
 * 
 */
public class GenerationTooLowException extends Exception implements ErrorRespose {

	private StoreAnswer answer;

	public GenerationTooLowException(StoreAnswer answer) {
		this.answer = answer;
	}

	public StoreAnswer getAnswer() {
		return answer;
	}

	@Override
	public ErrorType getErrorType() {
		return ErrorType.GEN_COUNTER_TOO_LOW;
	}
}
