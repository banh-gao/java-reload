package com.github.reload.net.data;

import com.github.reload.message.Content;
import com.github.reload.message.Header;
import com.github.reload.message.SecurityBlock;

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
}