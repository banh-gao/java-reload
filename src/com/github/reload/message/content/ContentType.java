package com.github.reload.message.content;

import io.netty.buffer.ByteBuf;
import java.util.EnumSet;
import com.github.reload.Context;
import com.github.reload.message.Content;
import com.github.reload.message.errors.ErrorMessageException;

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
		public Content decode(Context context, ByteBuf buf) {
			return new ProbeRequest(buf);
		}
	},
	PROBE_ANS((short) 2) {

		@Override
		public Content decode(Context context, ByteBuf buf) {
			return new ProbeAnswer(buf);
		}
	},
	ATTACH_REQ((short) 3) {

		@Override
		public Content decode(Context context, ByteBuf buf) {
			return new AttachReqAns(buf, this);
		}
	},
	ATTACH_ANS((short) 4) {

		@Override
		public Content decode(Context context, ByteBuf buf) {
			return new AttachReqAns(buf, this);
		}
	},
	STORE_REQ((short) 7) {

		@Override
		public Content decode(Context context, ByteBuf buf) {
			return new StoreRequest(context, buf);
		}
	},
	STORE_ANS((short) 8) {

		@Override
		public Content decode(Context context, ByteBuf buf) {
			return new StoreAnswer(context, buf);
		}
	},
	FETCH_REQ((short) 9) {

		@Override
		public Content decode(Context context, ByteBuf buf) {
			return new FetchRequest(context, buf);
		}
	},
	FETCH_ANS((short) 10) {

		@Override
		public Content decode(Context context, ByteBuf buf) {
			return new FetchAnswer(context, buf);
		}
	},
	FIND_REQ((short) 13) {

		@Override
		public Content decode(Context context, ByteBuf buf) {
			return new FindRequest(buf);
		}
	},
	FIND_ANS((short) 14) {

		@Override
		public Content decode(Context context, ByteBuf buf) {
			return new FindAnswer(buf);
		}
	},
	JOIN_REQ((short) 15) {

		@Override
		public Content decode(Context context, ByteBuf buf) {
			return JoinRequest.parseRequest(context, buf);
		}
	},
	JOIN_ANS((short) 16) {

		@Override
		public Content decode(Context context, ByteBuf buf) {
			return JoinAnswer.parseAnswer(context.getTopologyPlugin(), buf);
		}
	},
	LEAVE_REQ((short) 17) {

		@Override
		public Content decode(Context context, ByteBuf buf) {
			return LeaveRequest.parseRequest(context, buf);
		}
	},
	UPDATE_REQ((short) 19) {

		@Override
		public Content decode(Context context, ByteBuf buf) {
			Content content = context.getTopologyPlugin().parseUpdateRequest(buf);
			if (content.getType() != ContentType.UPDATE_REQ)
				throw new RuntimeException("The topology content must be of type " + ContentType.UPDATE_REQ);
			return content;
		}
	},
	UPDATE_ANS((short) 20) {

		@Override
		public Content decode(Context context, ByteBuf buf) {
			Content content = context.getTopologyPlugin().parseUpdateAnswer(buf);
			if (content.getType() != ContentType.UPDATE_ANS)
				throw new RuntimeException("The topology content must be of type " + ContentType.UPDATE_ANS);
			return content;
		}
	},
	ROUTE_QUERY_REQ((short) 21) {

		@Override
		public Content decode(Context context, ByteBuf buf) {
			return new RouteQueryRequest(buf);
		}
	},
	ROUTE_QUERY_ANS((short) 22) {

		@Override
		public Content decode(Context context, ByteBuf buf) {
			Content content = context.getTopologyPlugin().parseRouteQueryAnswer(buf);
			if (content.getType() != ContentType.ROUTE_QUERY_ANS)
				throw new RuntimeException("The topology content must be of type " + ContentType.ROUTE_QUERY_ANS);
			return content;
		}
	},
	PING_REQ((short) 23) {

		@Override
		public Content decode(Context context, ByteBuf buf) {
			return new PingRequest(buf);
		}
	},
	PING_ANS((short) 24) {

		@Override
		public Content decode(Context context, ByteBuf buf) {
			return new PingAnswer(buf);
		}
	},
	STAT_REQ((short) 25) {

		@Override
		public Content decode(Context context, ByteBuf buf) {
			return new StatRequest(context, buf);
		}
	},
	STAT_ANS((short) 26) {

		@Override
		public Content decode(Context context, ByteBuf buf) {
			return new StatAnswer(context, buf);
		}
	},
	APPATTACH_REQ((short) 29) {

		@Override
		public Content decode(Context context, ByteBuf buf) {
			return new AppAttachReqAns(buf, this);
		}
	},
	APPATTACH_ANS((short) 30) {

		@Override
		public Content decode(Context context, ByteBuf buf) {
			return new AppAttachReqAns(buf, this);
		}
	},
	CONFIG_UPDATE_REQ((short) 33) {

		@Override
		public Content decode(Context context, ByteBuf buf) {
			return new ConfigUpdateRequest(buf);
		}
	},
	CONFIG_UPDATE_ANS((short) 34) {

		@Override
		public Content decode(Context context, ByteBuf buf) {
			return new ConfigUpdateAnswer();
		}
	},
	PATH_TRACK_REQ((short) 101) {

		@Override
		public Content decode(Context context, ByteBuf buf) {
			return new PathTrackRequest(buf);
		}
	},
	PATH_TRACK_ANS((short) 102) {

		@Override
		public Content decode(Context context, ByteBuf buf) {
			return new PathTrackAnswer(buf);
		}
	},
	ERROR((short) 0xffff) {

		@Override
		public Content decode(Context context, ByteBuf buf) {
			return new Error(buf);
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
	public abstract Content decode(Context context, ByteBuf buf) throws ErrorMessageException;

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
