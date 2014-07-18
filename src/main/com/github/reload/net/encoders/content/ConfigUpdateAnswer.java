package com.github.reload.net.encoders.content;

import io.netty.buffer.ByteBuf;
import com.github.reload.Configuration;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.encoders.content.ConfigUpdateAnswer.ConfigUpdateAnsCodec;

@ReloadCodec(ConfigUpdateAnsCodec.class)
public class ConfigUpdateAnswer extends Content {

	@Override
	public ContentType getType() {
		return ContentType.CONFIG_UPDATE_ANS;
	}

	static class ConfigUpdateAnsCodec extends Codec<ConfigUpdateAnswer> {

		public ConfigUpdateAnsCodec(Configuration conf) {
			super(conf);
		}

		@Override
		public void encode(ConfigUpdateAnswer obj, ByteBuf buf, Object... params) throws com.github.reload.net.encoders.Codec.CodecException {
			// No data carried
		}

		@Override
		public ConfigUpdateAnswer decode(ByteBuf buf, Object... params) throws com.github.reload.net.encoders.Codec.CodecException {
			return new ConfigUpdateAnswer();
		}

	}

}
