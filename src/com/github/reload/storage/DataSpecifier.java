package com.github.reload.storage;

import java.math.BigInteger;
import javax.security.auth.login.Configuration;

/**
 * The specifier used to fetch data from the overlay
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class DataSpecifier {

	private static final int MODEL_SPEC_LENGTH_FIELD = EncUtils.U_INT16;

	public static final int DATA_SPEC_LENGTH_FIELD = KindId.MAX_LENGTH + EncUtils.U_INT64 + MODEL_SPEC_LENGTH_FIELD + (int) EncUtils.maxUnsignedInt(MODEL_SPEC_LENGTH_FIELD);

	private final DataKind kind;
	private BigInteger generation = BigInteger.ZERO;
	private final DataModelSpecifier modelSpecifier;

	DataSpecifier(DataKind kind, DataModelSpecifier spec) {
		this.kind = kind;
		modelSpecifier = spec;
	}

	public DataSpecifier(Configuration conf, UnsignedByteBuffer buf) throws UnknownKindException {
		KindId kindId = KindId.valueOf(buf);
		kind = conf.getDataKind(kindId);
		if (kind == null)
			throw new UnknownKindException(kindId);
		generation = buf.getSigned64();

		int length = buf.getLengthValue(MODEL_SPEC_LENGTH_FIELD);

		modelSpecifier = parseModelSpecifier(buf, length);
	}

	private DataModelSpecifier parseModelSpecifier(UnsignedByteBuffer buf, int length) {
		return kind.parseModelSpecifier(buf, length);
	}

	public DataKind getDataKind() {
		return kind;
	}

	public DataModelSpecifier getModelSpecifier() {
		return modelSpecifier;
	}

	public void setGeneration(BigInteger generation) {
		this.generation = generation;
	}

	public BigInteger getGeneration() {
		return generation;
	}

	public void writeTo(UnsignedByteBuffer buf) {
		kind.getKindId().writeTo(buf);
		buf.putUnsigned64(generation);

		Field lenFld = buf.allocateLengthField(MODEL_SPEC_LENGTH_FIELD);

		modelSpecifier.writeTo(buf);

		lenFld.setEncodedLength(buf.getConsumedFrom(lenFld.getNextPosition()));
	}
}
