package com.github.reload.message.content;


public class ConfigUpdateAnswer extends MessageContent {

	@Override
	protected void implWriteTo(UnsignedByteBuffer buf) {
		// No content for config answer
	}

	@Override
	public ContentType getType() {
		return ContentType.CONFIG_UPDATE_ANS;
	}

}
