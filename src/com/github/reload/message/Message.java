package com.github.reload.message;

/**
 * A RELOAD message
 */
public class Message {

	Header header;
	Content content;
	SecurityBlock secBlock;

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