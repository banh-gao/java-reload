package com.github.reload.services.storage.encoders;

import io.netty.buffer.ByteBuf;
import java.util.HashSet;
import java.util.Set;
import com.github.reload.conf.Configuration;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.encoders.content.Content;
import com.github.reload.net.encoders.content.ContentType;
import com.github.reload.services.storage.encoders.FindAnswer.FindAnswerCodec;

@ReloadCodec(FindAnswerCodec.class)
public class FindAnswer extends Content {

	private final Set<FindKindData> data;

	public FindAnswer(Set<FindKindData> data) {
		this.data = data;
	}

	@Override
	public ContentType getType() {
		return ContentType.FIND_ANS;
	}

	public Set<FindKindData> getData() {
		return data;
	}

	@Override
	public String toString() {
		return "FindAnswer [data=" + data + "]";
	}

	static class FindAnswerCodec extends Codec<FindAnswer> {

		private static final int LIST_LENGTH_FIELD = U_INT16;

		private final Codec<FindKindData> kindDataCodec;

		public FindAnswerCodec(Configuration conf) {
			super(conf);
			kindDataCodec = getCodec(FindKindData.class);
		}

		@Override
		public void encode(FindAnswer obj, ByteBuf buf, Object... params) throws CodecException {
			Field lenFld = allocateField(buf, LIST_LENGTH_FIELD);

			for (FindKindData d : obj.data) {
				kindDataCodec.encode(d, buf);
			}

			lenFld.updateDataLength();
		}

		@Override
		public FindAnswer decode(ByteBuf buf, Object... params) throws CodecException {
			ByteBuf kindDataBuf = readField(buf, LIST_LENGTH_FIELD);

			Set<FindKindData> kindData = new HashSet<FindKindData>();

			while (kindDataBuf.readableBytes() > 0) {
				kindData.add(kindDataCodec.decode(kindDataBuf));
			}

			kindDataBuf.release();

			return new FindAnswer(kindData);
		}

	}
}
