package com.github.reload.net.codecs.header;

import io.netty.buffer.ByteBuf;
import java.util.EnumSet;
import dagger.ObjectGraph;
import com.github.reload.net.codecs.Codec;
import com.github.reload.net.codecs.Codec.ReloadCodec;
import com.github.reload.net.codecs.header.ForwardingOption.ForwardingOptionCodec;

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

	static class ForwardingOptionCodec extends Codec<ForwardingOption> {

		private static final int OPTION_LENGTH_FIELD = U_INT16;

		private static final byte FWD_CRITICAL_MASK = 0x01;
		private static final byte DST_CRITICAL_MASK = 0x02;
		private static final byte RES_COPY_MASK = 0x04;

		private final Codec<UnknownForwardingOption> unknownFwdCodec;

		public ForwardingOptionCodec(ObjectGraph ctx) {
			super(ctx);
			unknownFwdCodec = getCodec(UnknownForwardingOption.class);
		}

		@Override
		public void encode(ForwardingOption obj, ByteBuf buf, Object... params) throws CodecException {
			buf.writeByte(obj.getType().code);
			buf.writeByte(getFlags(obj));

			Field lenFld = allocateField(buf, OPTION_LENGTH_FIELD);

			switch (obj.getType()) {
				case UNKNOWN_OPTION :
					unknownFwdCodec.encode((UnknownForwardingOption) obj, buf);
					break;

				default :
					break;
			}

			lenFld.updateDataLength();
		}

		private byte getFlags(ForwardingOption obj) {
			byte flags = 0;
			if (obj.isForwardCritical()) {
				flags = FWD_CRITICAL_MASK;
			}
			if (obj.isDestinationCritical()) {
				flags |= DST_CRITICAL_MASK;
			}
			if (obj.isResponseCopy()) {
				flags |= RES_COPY_MASK;
			}
			return flags;
		}

		@Override
		public ForwardingOption decode(ByteBuf buf, Object... params) throws CodecException {
			ForwardingOptionType type = ForwardingOptionType.valueOf(buf.readByte());

			ForwardingOption option = null;

			byte flags = buf.readByte();

			boolean isFwdCritical = (flags & FWD_CRITICAL_MASK) == FWD_CRITICAL_MASK;
			boolean isDstCritical = (flags & DST_CRITICAL_MASK) == DST_CRITICAL_MASK;
			boolean isRspCopy = (flags & RES_COPY_MASK) == RES_COPY_MASK;

			ByteBuf data = readField(buf, OPTION_LENGTH_FIELD);

			switch (type) {
				case UNKNOWN_OPTION :
					option = unknownFwdCodec.decode(data);
					break;
					// May be extended
			}

			assert (option != null);

			option.isForwardCritical = isFwdCritical;
			option.isDestinationCritical = isDstCritical;
			option.isResponseCopy = isRspCopy;

			return option;
		}
	}
}
