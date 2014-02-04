package com.github.reload.message.content;

import net.sf.jReload.message.ContentType;
import net.sf.jReload.message.MessageContent;
import net.sf.jReload.message.UnsignedByteBuffer;

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
