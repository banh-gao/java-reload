package com.github.reload.message;

import java.util.EnumSet;

/**
 * Forwarding option contained in the message header
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
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

	protected abstract ForwardingOptionType getType();

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
