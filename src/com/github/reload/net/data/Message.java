package com.github.reload.net.data;

import com.github.reload.message.Content;
import com.github.reload.message.Header;
import com.github.reload.message.SecurityBlock;
import com.github.reload.message.content.PingRequest;

/**
 * A RELOAD message
 */
public class Message {

	Header header;
	Content content;
	SecurityBlock secBlock;

	public Message() {
		header = new Header();
		content = new PingRequest(27);
		secBlock = new SecurityBlock();
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