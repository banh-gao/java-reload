package com.github.reload.message.content;

import io.netty.buffer.ByteBuf;
import java.math.BigInteger;
import com.github.reload.Configuration;
import com.github.reload.message.Codec;
import com.github.reload.message.Content;
import com.github.reload.message.ContentType;
import com.github.reload.message.ReloadCodec;
import com.github.reload.message.content.PingAnswer.PingAnswerCodec;

@ReloadCodec(PingAnswerCodec.class)
public class PingAnswer extends Content {

	private final long responseId;
	private final BigInteger responseTime;

	public PingAnswer(long responseId, BigInteger responseTime) {
		super();
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

	public static class PingAnswerCodec extends Codec<PingAnswer> {

		public PingAnswerCodec(Configuration conf) {
			super(conf);
		}

		@Override
		public void encode(PingAnswer obj, ByteBuf buf, Object... params) throws com.github.reload.message.Codec.CodecException {
			buf.writeLong(obj.responseId);
			buf.writeBytes(obj.responseTime.toByteArray());
		}

		@Override
		public PingAnswer decode(ByteBuf buf, Object... params) throws com.github.reload.message.Codec.CodecException {
			long responseId = buf.readLong();
			byte[] resposeTimeData = new byte[U_INT64];
			buf.readBytes(resposeTimeData);
			BigInteger responseTime = new BigInteger(1, resposeTimeData);

			return new PingAnswer(responseId, responseTime);
		}

	}
}
