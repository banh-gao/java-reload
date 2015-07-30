package com.github.reload.net.codecs.content;

import io.netty.buffer.ByteBuf;
import dagger.ObjectGraph;
import com.github.reload.net.codecs.Codec;
import com.github.reload.net.codecs.Codec.ReloadCodec;
import com.github.reload.net.codecs.content.ConfigUpdateAnswer.ConfigUpdateAnsCodec;

@ReloadCodec(ConfigUpdateAnsCodec.class)
public class ConfigUpdateAnswer extends Content {

	@Override
	public ContentType getType() {
		return ContentType.CONFIG_UPDATE_ANS;
	}

	static class ConfigUpdateAnsCodec extends Codec<ConfigUpdateAnswer> {

		public ConfigUpdateAnsCodec(ObjectGraph ctx) {
			super(ctx);
		}

		@Override
		public void encode(ConfigUpdateAnswer obj, ByteBuf buf, Object... params) throws com.github.reload.net.codecs.Codec.CodecException {
			// No data carried
		}

		@Override
		public ConfigUpdateAnswer decode(ByteBuf buf, Object... params) throws com.github.reload.net.codecs.Codec.CodecException {
			return new ConfigUpdateAnswer();
		}

	}

}
