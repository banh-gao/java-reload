package com.github.reload.message.content;

import io.netty.buffer.ByteBuf;
import com.github.reload.Configuration;
import com.github.reload.message.Codec;
import com.github.reload.message.Codec.ReloadCodec;
import com.github.reload.message.Content;
import com.github.reload.message.ContentType;
import com.github.reload.message.content.LeaveAnswer.LeaveAnswerCodec;

@ReloadCodec(LeaveAnswerCodec.class)
public class LeaveAnswer extends Content {

	@Override
	public ContentType getType() {
		return ContentType.LEAVE_ANS;
	}

	public static class LeaveAnswerCodec extends Codec<LeaveAnswer> {

		public LeaveAnswerCodec(Configuration conf) {
			super(conf);
		}

		@Override
		public void encode(LeaveAnswer obj, ByteBuf buf, Object... params) throws com.github.reload.message.Codec.CodecException {
			// No data carried
		}

		@Override
		public LeaveAnswer decode(ByteBuf buf, Object... params) throws com.github.reload.message.Codec.CodecException {
			return new LeaveAnswer();
		}

	}

}
