package com.github.reload.message;

import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import com.github.reload.net.data.ReloadCodec;

/**
 * RELOAD message content
 */
@ReloadCodec(ContentCodec.class)
public abstract class Content {

	List<MessageExtension> messageExtensions = new ArrayList<MessageExtension>();

	public abstract ContentType getType();

	/**
	 * Append a message extension to the message
	 * 
	 * @param extension
	 *            The extension to be appended
	 */
	public void appendMessageExtension(MessageExtension extension) {
		messageExtensions.add(extension);
	}

	public List<MessageExtension> getMessageExtensions() {
		return messageExtensions;
	}

	/**
	 * @return the certificates needed to authenticate content values such as
	 *         storage signatures
	 */
	public Set<? extends Certificate> getNeededCertificates() {
		return Collections.emptySet();
	}

	public boolean isRequest() {
		return getType().isRequest();
	}

	public boolean isAnswer() {
		return getType().isAnswer();
	}
}
