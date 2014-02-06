package com.github.reload.net.ice;

import net.sf.jReload.message.EncUtils;
import net.sf.jReload.message.UnsignedByteBuffer;
import net.sf.jReload.message.UnsignedByteBuffer.Field;

public class IceExtension {

	private static final int NAME_LENGTH_FIELD = EncUtils.U_INT16;
	private static final int VALUE_LENGTH_FIELD = EncUtils.U_INT16;

	private final byte[] name;
	private final byte[] value;

	public IceExtension(UnsignedByteBuffer buf) {
		name = new byte[buf.getLengthValue(NAME_LENGTH_FIELD)];
		buf.getRaw(name);
		value = new byte[buf.getLengthValue(VALUE_LENGTH_FIELD)];
		buf.getRaw(value);
	}

	public int getLength() {
		return NAME_LENGTH_FIELD + name.length + VALUE_LENGTH_FIELD + value.length;
	}

	public void writeTo(UnsignedByteBuffer buf) {
		Field nameLenFld = buf.allocateLengthField(NAME_LENGTH_FIELD);
		buf.putRaw(name);
		nameLenFld.setEncodedLength(buf.getConsumedFrom(nameLenFld.getNextPosition()));

		Field valLenFld = buf.allocateLengthField(VALUE_LENGTH_FIELD);
		buf.putRaw(value);
		valLenFld.setEncodedLength(buf.getConsumedFrom(valLenFld.getNextPosition()));
	}
}
