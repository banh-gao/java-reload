package com.github.reload.net.codecs;

import com.github.reload.net.codecs.content.Content;
import com.github.reload.net.codecs.secBlock.SecurityBlock;
import com.google.common.base.Objects;

/**
 * A RELOAD message
 */
public class Message {

	Header header;
	Content content;
	SecurityBlock secBlock;

	public Message(Header header, Content content, SecurityBlock secBlock) {
		this.header = header;
		this.content = content;
		this.secBlock = secBlock;
	}

	public Header getHeader() {
		return header;
	}

	public Content getContent() {
		return content;
	}

	public SecurityBlock getSecBlock() {
		return secBlock;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this).add("header", header).add("content", content).add("secBlock", secBlock).toString();
	}

}