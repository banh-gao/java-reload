package com.github.reload.net.codecs.content;

import io.netty.buffer.ByteBuf;
import dagger.ObjectGraph;
import com.github.reload.net.codecs.Codec;
import com.github.reload.net.codecs.Codec.ReloadCodec;
import com.github.reload.net.codecs.content.LeaveAnswer.LeaveAnswerCodec;

@ReloadCodec(LeaveAnswerCodec.class)
public class LeaveAnswer extends Content {

	@Override
	public ContentType getType() {
		return ContentType.LEAVE_ANS;
	}

	static class LeaveAnswerCodec extends Codec<LeaveAnswer> {

		public LeaveAnswerCodec(ObjectGraph ctx) {
			super(ctx);
		}

		@Override
		public void encode(LeaveAnswer obj, ByteBuf buf, Object... params) throws com.github.reload.net.codecs.Codec.CodecException {
			// No data carried
		}

		@Override
		public LeaveAnswer decode(ByteBuf buf, Object... params) throws com.github.reload.net.codecs.Codec.CodecException {
			return new LeaveAnswer();
		}

	}

}
