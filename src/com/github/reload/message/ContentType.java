package com.github.reload.message;

import java.util.EnumSet;
import com.github.reload.message.content.AppAttachReqAns;
import com.github.reload.message.content.AttachMessage;
import com.github.reload.message.content.ConfigUpdateAnswer;
import com.github.reload.message.content.ConfigUpdateRequest;
import com.github.reload.message.content.JoinAnswer;
import com.github.reload.message.content.JoinRequest;
import com.github.reload.message.content.LeaveRequest;
import com.github.reload.message.content.PingAnswer;
import com.github.reload.message.content.PingRequest;
import com.github.reload.message.content.ProbeAnswer;
import com.github.reload.message.content.ProbeRequest;
import com.github.reload.message.content.RouteQueryRequest;
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
	PROBE_REQ((short) 1) {

		@Override
		public Class<? extends Content> getContentClass() {
			return ProbeRequest.class;
		}
	},
	PROBE_ANS((short) 2) {

		@Override
		public Class<? extends Content> getContentClass() {
			return ProbeAnswer.class;
		}
	},
	ATTACH_REQ((short) 3) {

		@Override
		public Class<? extends Content> getContentClass() {
			return AttachMessage.class;
		}
	},
	ATTACH_ANS((short) 4) {

		@Override
		public Class<? extends Content> getContentClass() {
			return AttachMessage.class;
		}
	},
	STORE_REQ((short) 7) {

		@Override
		public Class<? extends Content> getContentClass() {
			return StoreRequest.class;
		}
	},
	STORE_ANS((short) 8) {

		@Override
		public Class<? extends Content> getContentClass() {
			return StoreAnswer.class;
		}
	},
	FETCH_REQ((short) 9) {

		@Override
		public Class<? extends Content> getContentClass() {
			return FetchRequest.class;
		}
	},
	FETCH_ANS((short) 10) {

		@Override
		public Class<? extends Content> getContentClass() {
			return FetchAnswer.class;
		}
	},
	FIND_REQ((short) 13) {

		@Override
		public Class<? extends Content> getContentClass() {
			return FindRequest.class;
		}
	},
	FIND_ANS((short) 14) {

		@Override
		public Class<? extends Content> getContentClass() {
			return FindAnswer.class;
		}
	},
	JOIN_REQ((short) 15) {

		@Override
		public Class<? extends Content> getContentClass() {
			return JoinRequest.class;
		}
	},
	JOIN_ANS((short) 16) {

		@Override
		public Class<? extends Content> getContentClass() {
			return JoinAnswer.class;
		}
	},
	LEAVE_REQ((short) 17) {

		@Override
		public Class<? extends Content> getContentClass() {
			return LeaveRequest.class;
		}
	},
	UPDATE_REQ((short) 19) {

		@Override
		public MessageContent parseContent(Context context, UnsignedByteBuffer buf) {
			MessageContent content = context.getTopologyPlugin().parseUpdateRequest(buf);
			if (content.getType() != ContentType.UPDATE_REQ)
				throw new RuntimeException("The topology content must be of type " + ContentType.UPDATE_REQ);
			return content;
		}

		@Override
		public Class<? extends Content> getContentClass() {
			// TODO Auto-generated method stub
			return null;
		}
	},
	UPDATE_ANS((short) 20) {

		@Override
		public MessageContent parseContent(Context context, UnsignedByteBuffer buf) {
			MessageContent content = context.getTopologyPlugin().parseUpdateAnswer(buf);
			if (content.getType() != ContentType.UPDATE_ANS)
				throw new RuntimeException("The topology content must be of type " + ContentType.UPDATE_ANS);
			return content;
		}

		@Override
		public Class<? extends Content> getContentClass() {
			// TODO Auto-generated method stub
			return null;
		}
	},
	ROUTE_QUERY_REQ((short) 21) {

		@Override
		public Class<? extends Content> getContentClass() {
			return RouteQueryRequest.class;
		}
	},
	ROUTE_QUERY_ANS((short) 22) {

		@Override
		public MessageContent parseContent(Context context, UnsignedByteBuffer buf) {
			MessageContent content = context.getTopologyPlugin().parseRouteQueryAnswer(buf);
			if (content.getType() != ContentType.ROUTE_QUERY_ANS)
				throw new RuntimeException("The topology content must be of type " + ContentType.ROUTE_QUERY_ANS);
			return content;
		}

		@Override
		public Class<? extends Content> getContentClass() {
			// TODO Auto-generated method stub
			return null;
		}
	},
	PING_REQ((short) 23) {

		@Override
		public Class<? extends Content> getContentClass() {
			return PingRequest.class;
		}
	},
	PING_ANS((short) 24) {

		@Override
		public Class<? extends Content> getContentClass() {
			return PingAnswer.class;
		}
	},
	STAT_REQ((short) 25) {

		@Override
		public Class<? extends Content> getContentClass() {
			return StatRequest.class;
		}
	},
	STAT_ANS((short) 26) {

		@Override
		public Class<? extends Content> getContentClass() {
			return StatAnswer.class;
		}
	},
	APPATTACH_REQ((short) 29) {

		@Override
		public Class<? extends Content> getContentClass() {
			return AppAttachReqAns.class;
		}
	},
	APPATTACH_ANS((short) 30) {

		@Override
		public Class<? extends Content> getContentClass() {
			return AppAttachReqAns.class;
		}
	},
	CONFIG_UPDATE_REQ((short) 33) {

		@Override
		public Class<? extends Content> getContentClass() {
			return ConfigUpdateRequest.class;
		}
	},
	CONFIG_UPDATE_ANS((short) 34) {

		@Override
		public Class<? extends Content> getContentClass() {
			return ConfigUpdateAnswer.class;
		}
	},
	ERROR((short) 0xffff) {

		@Override
		public Class<? extends Content> getContentClass() {
			return Error.class;
		}
	};

	private final short code;

	private ContentType(short code) {
		this.code = code;
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
	 * Parses the message content for this message type
	 */
	public abstract Class<? extends Content> getContentClass();

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
