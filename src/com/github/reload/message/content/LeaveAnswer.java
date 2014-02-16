package com.github.reload.message.content;

import io.netty.buffer.ByteBuf;
import com.github.reload.Context;
import com.github.reload.message.Content;
import com.github.reload.message.ContentType;
import com.github.reload.message.content.LeaveAnswer.LeaveAnswerCodec;
import com.github.reload.net.data.Codec;
import com.github.reload.net.data.ReloadCodec;

@ReloadCodec(LeaveAnswerCodec.class)
public class LeaveAnswer extends Content {

	@Override
	public ContentType getType() {
		return ContentType.LEAVE_ANS;
	}

	public static class LeaveAnswerCodec extends Codec<LeaveAnswer> {

		public LeaveAnswerCodec(Context context) {
			super(context);
		}

		@Override
		public void encode(LeaveAnswer obj, ByteBuf buf) throws com.github.reload.net.data.Codec.CodecException {
			// No data carried
		}

		@Override
		public LeaveAnswer decode(ByteBuf buf) throws com.github.reload.net.data.Codec.CodecException {
			return new LeaveAnswer();
		}

	}

}
