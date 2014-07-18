package com.github.reload.net.encoders.content;

import io.netty.buffer.ByteBuf;
import java.math.BigInteger;
import com.github.reload.Configuration;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.encoders.content.PingAnswer.PingAnswerCodec;

@ReloadCodec(PingAnswerCodec.class)
public class PingAnswer extends Content {

	private final long responseId;
	private final BigInteger responseTime;

	public PingAnswer(long responseId, BigInteger responseTime) {
		this.responseId = responseId;
		this.responseTime = responseTime;
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
	public ContentType getType() {
		return ContentType.PING_ANS;
	}

	@Override
	public String toString() {
		return "PingAnswer [responseId=" + responseId + ", responseTime=" + responseTime + "]";
	}

	static class PingAnswerCodec extends Codec<PingAnswer> {

		private static final int RESPONSE_TIME_SIZE = U_INT64;

		public PingAnswerCodec(Configuration conf) {
			super(conf);
		}

		@Override
		public void encode(PingAnswer obj, ByteBuf buf, Object... params) throws CodecException {
			buf.writeLong(obj.responseId);

			if (obj.responseTime.compareTo(BigInteger.ZERO) < 0 || obj.responseTime.bitCount() > 8 * RESPONSE_TIME_SIZE)
				throw new CodecException("Invalid response time");

			byte[] respTime = obj.responseTime.toByteArray();

			// Add zeros padding to response time to ensure the field has always
			// the size specified in RESPONSE_TIME_SIZE
			buf.writeZero(RESPONSE_TIME_SIZE - respTime.length);

			buf.writeBytes(respTime);
		}

		@Override
		public PingAnswer decode(ByteBuf buf, Object... params) throws CodecException {
			long responseId = buf.readLong();
			byte[] resposeTimeData = new byte[RESPONSE_TIME_SIZE];
			buf.readBytes(resposeTimeData);
			BigInteger responseTime = new BigInteger(1, resposeTimeData);

			return new PingAnswer(responseId, responseTime);
		}

	}
}
