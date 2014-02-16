package com.github.reload.message;

import java.util.EnumSet;
import com.github.reload.message.content.*;
import com.github.reload.message.errors.Error;
import com.github.reload.storage.FetchAnswer;
import com.github.reload.storage.FetchRequest;
import com.github.reload.storage.FindAnswer;
import com.github.reload.storage.FindRequest;
import com.github.reload.storage.StatAnswer;
import com.github.reload.storage.StatRequest;
import com.github.reload.storage.StoreAnswer;
import com.github.reload.storage.StoreRequest;

/**
 * Message type codes, also used to create message content object instances for
 * the corresponding type
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public enum ContentType {
	PROBE_REQ((short) 1,ProbeRequest.class),
	PROBE_ANS((short) 2,ProbeAnswer.class),
	ATTACH_REQ((short) 3,AttachMessage.class),
	ATTACH_ANS((short) 4,AttachMessage.class),
	STORE_REQ((short) 7,StoreRequest.class),
	STORE_ANS((short) 8,StoreAnswer.class),
	FETCH_REQ((short) 9,FetchRequest.class),
	FETCH_ANS((short) 10,FetchAnswer.class),
	FIND_REQ((short) 13,FindRequest.class),
	FIND_ANS((short) 14,FindAnswer.class),
	JOIN_REQ((short) 15,JoinRequest.class),
	JOIN_ANS((short) 16,JoinAnswer.class),
	LEAVE_REQ((short) 17,LeaveRequest.class),
	UPDATE_REQ((short) 19,UpdateRequest.class),
	UPDATE_ANS((short) 20, UpdateAnswer.class),
	ROUTE_QUERY_REQ((short) 21, RouteQueryRequest.class),
	ROUTE_QUERY_ANS((short) 22), RouteQueryAnswer.class),
	PING_REQ((short) 23, PingRequest.class),
	PING_ANS((short) 24, PingAnswer.class),
	STAT_REQ((short) 25, StatRequest.class),
	STAT_ANS((short) 26, StatAnswer.class),
	APPATTACH_REQ((short) 29, AppAttachReqAns.class),
	APPATTACH_ANS((short) 30, AppAttachReqAns.class),
	CONFIG_UPDATE_REQ((short) 33, ConfigUpdateRequest.class),
	CONFIG_UPDATE_ANS((short) 34, ConfigUpdateAnswer.class),
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
