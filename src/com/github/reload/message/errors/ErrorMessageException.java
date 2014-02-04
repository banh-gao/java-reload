package com.github.reload.message.errors;


/**
 * Indicates that an error message was received
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public abstract class ErrorMessageException extends Exception {

	private final Error error;

	protected static final int INFO_LENGTH_FIELD = EncUtils.U_INT16;

	protected ErrorMessageException(Error error) {
		super(error.getStringInfo());
		this.error = error;
	}

	public Error getError() {
		return error;
	}
}
