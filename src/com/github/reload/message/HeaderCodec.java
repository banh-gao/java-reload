package com.github.reload.message;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import com.github.reload.Context;
import com.github.reload.net.data.CodecUtils;

/**
 * RELOAD Header codec
 */
public class HeaderCodec extends AbstractCodec<Header> {

	private static final int RELOAD_TOKEN = (int) 0xd2454c4fL;

	public static final int TOTAL_MESSAGE_LENGTH_FIELD = CodecUtils.U_INT32;

	// First two bytes initial value
	private static final short FRAGMENT_INITIAL_VALUE = (short) 0x8000;
	// Second bit set means this is the last fragment
	private static final short LAST_FRAGMENT_MASK = (short) 0x4000;

	private static final int LISTS_LENGTH_FIELD = CodecUtils.U_INT16;

	/**
	 * Size in bytes of the first part of the header from the beginning to the
	 * message length field
	 */
	public static int HDR_LEADING_LEN = 128;

	private static final short HEADER_MIN_LENGTH = 38;

	private CodecFactory factory;

	@Override
	public void init(Context ctx, CodecFactory factory) {
		this.factory = factory;
	}

	@Override
	public void encode(Header h, ByteBuf buf) {
		buf.writeInt(RELOAD_TOKEN);
		buf.writeInt(h.overlayHash);
		buf.writeShort(h.configurationSequence);
		buf.writeByte(h.version);
		buf.writeByte(h.ttl);
		buf.writeShort(getFragmentHeadValue(h));
		buf.writeShort(h.fragmentOffset);

		// Allocate space for message length field
		buf.writerIndex(buf.writerIndex() + TOTAL_MESSAGE_LENGTH_FIELD);

		buf.writeLong(h.transactionId);
		buf.writeInt(h.maxResponseLength);

		// Allocate space for length fields for all the 3 lists
		int viaLengthFld = buf.writerIndex();
		buf.writerIndex(buf.writerIndex() + LISTS_LENGTH_FIELD);
		int destLengthFld = buf.writerIndex();
		buf.writerIndex(buf.writerIndex() + LISTS_LENGTH_FIELD);
		int fwdLengthFld = buf.writerIndex();
		buf.writerIndex(buf.writerIndex() + LISTS_LENGTH_FIELD);

		int viaStartPos = buf.writerIndex();
		factory.getCodec(DestinationList.class).encode(h.viaList, buf);
		setListLength(buf, viaStartPos, viaLengthFld);

		int destStartPos = buf.writerIndex();
		factory.getCodec(DestinationList.class).encode(h.destinationList, buf);
		setListLength(buf, destStartPos, destLengthFld);

		int fwdStartPos = buf.writerIndex();
		writeFwdOptions(buf, h);
		setListLength(buf, fwdStartPos, fwdLengthFld);
	}

	/**
	 * @return the first two bytes of fragment field
	 */
	private short getFragmentHeadValue(Header h) {
		short frags = FRAGMENT_INITIAL_VALUE;

		if (h.isLastFragment) {
			frags |= LAST_FRAGMENT_MASK;
		}
		return frags;
	}

	private void setListLength(ByteBuf buf, int listStartPos, int lengthFieldPos) {
		int listLength = buf.writerIndex() - listStartPos;
		buf.setShort(lengthFieldPos, listLength);
	}

	private void writeFwdOptions(ByteBuf buf, Header h) {
		for (ForwardingOption o : h.forwardingOptions) {
			factory.getCodec(ForwardingOption.class).encode(o, buf);
		}
	}

	@Override
	public Header decode(ByteBuf buf) {
		Header h = new Header();
		h.isReloTokenValid = (buf.readInt() == RELOAD_TOKEN);
		h.overlayHash = buf.readInt();
		h.configurationSequence = buf.readShort();
		h.version = buf.readShort();
		h.ttl = buf.readUnsignedByte();

		h.isLastFragment = (buf.readShort() & LAST_FRAGMENT_MASK) == LAST_FRAGMENT_MASK;
		h.fragmentOffset = buf.readUnsignedShort();

		int totalMessageLength = (int) buf.readUnsignedInt();

		h.transactionId = buf.readLong();

		h.maxResponseLength = buf.readInt();

		int viaListLength = buf.readShort();
		int destinationListLength = buf.readShort();
		int fwdOptionsLength = buf.readShort();

		h.viaList = factory.getCodec(DestinationList.class).decode(buf.slice(buf.readerIndex(), viaListLength));
		h.destinationList = factory.getCodec(DestinationList.class).decode(buf.slice(buf.readerIndex(), destinationListLength));
		h.forwardingOptions = decodeOptions(buf.slice(buf.readerIndex(), fwdOptionsLength));

		h.headerLength = HEADER_MIN_LENGTH + viaListLength + destinationListLength + fwdOptionsLength;
		h.payloadLength = totalMessageLength - h.headerLength;
		return h;
	}

	private List<ForwardingOption> decodeOptions(ByteBuf buf) {
		List<ForwardingOption> out = new ArrayList<ForwardingOption>();

		while (buf.readableBytes() > 0)
			out.add(factory.getCodec(ForwardingOption.class).decode(buf));

		return out;
	}

}
