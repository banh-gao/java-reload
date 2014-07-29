package com.github.reload.net.encoders.content;

import java.util.EnumSet;
import com.github.reload.storage.encoders.FetchAnswer;
import com.github.reload.storage.encoders.FetchRequest;
import com.github.reload.storage.encoders.FindAnswer;
import com.github.reload.storage.encoders.FindRequest;
import com.github.reload.storage.encoders.StatAnswer;
import com.github.reload.storage.encoders.StatRequest;
import com.github.reload.storage.encoders.StoreAnswer;
import com.github.reload.storage.encoders.StoreRequest;

/**
 * Message type codes, also used to create message content object instances for
 * the corresponding type
 * 
 */
public enum ContentType {
	UNKNOWN((short) 0, UpdateRequest.class),
	PROBE_REQ((short) 0x1, ProbeRequest.class),
	PROBE_ANS((short) 0x2, ProbeAnswer.class),
	ATTACH_REQ((short) 0x3, AttachMessage.class),
	ATTACH_ANS((short) 0x4, AttachMessage.class),
	STORE_REQ((short) 0x7, StoreRequest.class),
	STORE_ANS((short) 0x8, StoreAnswer.class),
	FETCH_REQ((short) 0x9, FetchRequest.class),
	FETCH_ANS((short) 0xa, FetchAnswer.class),
	FIND_REQ((short) 0xd, FindRequest.class),
	FIND_ANS((short) 0xe, FindAnswer.class),
	JOIN_REQ((short) 0xf, JoinRequest.class),
	JOIN_ANS((short) 0x10, JoinAnswer.class),
	LEAVE_REQ((short) 0x11, LeaveRequest.class),
	LEAVE_ANS((short) 0x12, LeaveAnswer.class),
	UPDATE_REQ((short) 0x13, UpdateRequest.class),
	UPDATE_ANS((short) 0x14, UpdateAnswer.class),
	ROUTE_QUERY_REQ((short) 0x15, RouteQueryRequest.class),
	ROUTE_QUERY_ANS((short) 0x16, RouteQueryAnswer.class),
	PING_REQ((short) 0x17, PingRequest.class),
	PING_ANS((short) 0x18, PingAnswer.class),
	STAT_REQ((short) 0x19, StatRequest.class),
	STAT_ANS((short) 0x1a, StatAnswer.class),
	APPATTACH_REQ((short) 0x1d, AppAttachMessage.class),
	APPATTACH_ANS((short) 0x1e, AppAttachMessage.class),
	CONFIG_UPDATE_REQ((short) 0x21, ConfigUpdateRequest.class),
	CONFIG_UPDATE_ANS((short) 0x22, ConfigUpdateAnswer.class),
	ERROR((short) 0xffff, Error.class);

	private final short code;
	private final Class<? extends Content> contentClass;

	private ContentType(short code, Class<? extends Content> contentClass) {
		this.code = code;
		this.contentClass = contentClass;
	}

	public static ContentType valueOf(short code) {
		for (ContentType mc : EnumSet.allOf(ContentType.class))
			if (mc.code == code)
				return mc;
		return null;
	}

	public short getCode() {
		return code;
	}

	/**
	 * @return the content class that implements this message type
	 */
	public Class<? extends Content> getContentClass() {
		return contentClass;
	}

	public boolean isRequest() {
		if (this == ContentType.ERROR)
			return false;
		return (code % 2 != 0);
	}

	public boolean isAnswer() {
		return !isRequest();
	}

	/**
	 * @return True if this content type is an answer for the specified request
	 *         type
	 */
	public boolean isAnswerFor(ContentType requestType) {
		if (requestType.isAnswer() || isRequest())
			return false;

		return requestType.code == code - 1;
	}
}
