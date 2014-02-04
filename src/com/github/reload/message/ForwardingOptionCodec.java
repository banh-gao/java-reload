package com.github.reload.message;

import io.netty.buffer.ByteBuf;
import javax.lang.model.type.ErrorType;
import com.github.reload.Context;
import com.github.reload.message.ForwardingOption.ForwardingOptionType;
import com.github.reload.message.errors.ErrorMessageException;
import com.github.reload.net.data.CodecUtils;
import com.github.reload.net.data.CodecUtils.Field;

public class ForwardingOptionCodec extends AbstractCodec<ForwardingOption> {

	private static final int OPTION_LENGTH_FIELD = CodecUtils.U_INT16;

	private static final byte FWD_CRITICAL_MASK = 0x01;
	private static final byte DST_CRITICAL_MASK = 0x02;
	private static final byte RES_COPY_MASK = 0x04;

	private CodecFactory factory;

	@Override
	public void init(Context ctx, CodecFactory factory) {
		this.factory = factory;
	}

	@Override
	public void encode(ForwardingOption obj, ByteBuf buf) {
		buf.writeByte(obj.getType().code);
		buf.writeByte(getFlags(obj));

		Field lenFld = CodecUtils.allocateField(buf, OPTION_LENGTH_FIELD);

		factory.getCodec(obj.getClass()).encode(obj, buf);

		lenFld.updateDataLength();
	}

	private byte getFlags(ForwardingOption obj) {
		byte flags = 0;
		if (obj.isForwardCritical) {
			flags = FWD_CRITICAL_MASK;
		}
		if (obj.isDestinationCritical) {
			flags |= DST_CRITICAL_MASK;
		}
		if (obj.isResponseCopy) {
			flags |= RES_COPY_MASK;
		}
		return flags;
	}

	@Override
	public ForwardingOption decode(ByteBuf buf) {
		ForwardingOptionType type = ForwardingOptionType.valueOf(buf.readByte());

		ForwardingOption option = null;

		byte flags = buf.readByte();

		boolean isFwdCritical = (flags & FWD_CRITICAL_MASK) == FWD_CRITICAL_MASK;
		boolean isDstCritical = (flags & DST_CRITICAL_MASK) == DST_CRITICAL_MASK;
		boolean isRspCopy = (flags & RES_COPY_MASK) == RES_COPY_MASK;

		ByteBuf data = CodecUtils.readData(buf, OPTION_LENGTH_FIELD);

		if (type == ForwardingOptionType.UNKNOWN_OPTION && (isFwdCritical || isDstCritical))
			throw new UnsupportedFwdOptionException("Unknown forwarding option");

		switch (type) {
			case UNKNOWN_OPTION :
				option = decodeUnknown(data);
				break;
		// May be extended
		}

		assert (option != null);

		option.isForwardCritical = isFwdCritical;
		option.isDestinationCritical = isDstCritical;
		option.isResponseCopy = isRspCopy;

		return option;
	}

	private ForwardingOption decodeUnknown(final ByteBuf data) {
		return new ForwardingOption() {

			@Override
			protected void implEncode(ByteBuf buf) {
				buf.writeBytes(data);
			}

			@Override
			protected ForwardingOptionType getType() {
				return ForwardingOptionType.UNKNOWN_OPTION;
			}
		};
	}

	/**
	 * Indicates an unsupported forwarding option critical for header processing
	 * 
	 * @author Daniel Zozin <zdenial@gmx.com>
	 * 
	 */
	public static class UnsupportedFwdOptionException extends ErrorMessageException {

		public UnsupportedFwdOptionException(String message) {
			super(new Error(ErrorType.UNSUPPORTED_FWD_OPTION, message));
		}

	}
}
