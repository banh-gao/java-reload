package com.github.reload.net.encoders.header;

import java.util.EnumSet;
import com.github.reload.net.encoders.Codec.ReloadCodec;

/**
 * Forwarding option contained in the message header
 * 
 */
@ReloadCodec(ForwardingOptionCodec.class)
public abstract class ForwardingOption {

	public enum ForwardingOptionType {
		UNKNOWN_OPTION((byte) 0);

		final byte code;

		private ForwardingOptionType(byte code) {
			this.code = code;
		}

		public static ForwardingOptionType valueOf(byte code) {
			for (ForwardingOptionType t : EnumSet.allOf(ForwardingOptionType.class))
				if (t.code == code)
					return t;
			return UNKNOWN_OPTION;
		}
	}

	public abstract ForwardingOptionType getType();

	boolean isForwardCritical;
	boolean isDestinationCritical;
	boolean isResponseCopy;

	public boolean isDestinationCritical() {
		return isDestinationCritical;
	}

	public boolean isForwardCritical() {
		return isForwardCritical;
	}

	public boolean isResponseCopy() {
		return isResponseCopy;
	}
}
