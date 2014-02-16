package com.github.reload.message.content;

import java.math.BigInteger;
import java.util.Random;

public class PingAnswer extends MessageContent {

	private static final Random idGen = new Random(System.currentTimeMillis());

	private final long responseId;
	private final BigInteger responseTime;

	public PingAnswer(UnsignedByteBuffer buf) {
		responseId = buf.getRaw64();
		responseTime = buf.getSigned64();
	}

	public PingAnswer() {
		responseId = idGen.nextLong();
		responseTime = BigInteger.valueOf(System.currentTimeMillis());
	}

	/**
	 * @return The identifier of the ping response
	 */
	public long getResponseId() {
		return responseId;
	}

	/**
	 * @return The time this answer was generated in UNIX time format
	 */
	public BigInteger getResponseTime() {
		return responseTime;
	}

	@Override
	protected void implWriteTo(UnsignedByteBuffer buf) {
		buf.putRaw64(responseId);
		buf.putUnsigned64(responseTime);
	}

	@Override
	public ContentType getType() {
		return ContentType.PING_ANS;
	}

	@Override
	public String toString() {
		return "PingAnswer [responseId=" + responseId + ", responseTime=" + responseTime + "]";
	}
}
