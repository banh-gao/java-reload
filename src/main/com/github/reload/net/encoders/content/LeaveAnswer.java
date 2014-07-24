package com.github.reload.net.encoders.content;

import io.netty.buffer.ByteBuf;
import com.github.reload.conf.Configuration;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.encoders.content.LeaveAnswer.LeaveAnswerCodec;

@ReloadCodec(LeaveAnswerCodec.class)
public class LeaveAnswer extends Content {

	@Override
	public ContentType getType() {
		return ContentType.LEAVE_ANS;
	}

	static class LeaveAnswerCodec extends Codec<LeaveAnswer> {

		public LeaveAnswerCodec(Configuration conf) {
			super(conf);
		}

		@Override
		public void encode(LeaveAnswer obj, ByteBuf buf, Object... params) throws com.github.reload.net.encoders.Codec.CodecException {
			// No data carried
		}

		@Override
		public LeaveAnswer decode(ByteBuf buf, Object... params) throws com.github.reload.net.encoders.Codec.CodecException {
			return new LeaveAnswer();
		}

	}

}
