package com.github.reload.services.storage.net;

import io.netty.buffer.ByteBuf;
import java.math.BigInteger;
import java.util.Objects;
import com.github.reload.components.ComponentsContext;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.services.storage.DataKind;
import com.github.reload.services.storage.DataModel.ValueSpecifier;
import com.github.reload.services.storage.net.StoreKindSpecifier.StoreKindSpecifierCodec;

@ReloadCodec(StoreKindSpecifierCodec.class)
public class StoreKindSpecifier {

	private DataKind kind;
	private ValueSpecifier valueSpecifier;

	private BigInteger generation = BigInteger.ZERO;

	public void setKind(DataKind kind) {
		this.kind = kind;
	}

	public void setValueSpecifier(ValueSpecifier valueSpecifier) {
		this.valueSpecifier = valueSpecifier;
	}

	public DataKind getKind() {
		return kind;
	}

	public ValueSpecifier getValueSpecifier() {
		return valueSpecifier;
	}

	public BigInteger getGeneration() {
		return generation;
	}

	public void setGeneration(BigInteger generation) {
		this.generation = generation;
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), kind, generation, valueSpecifier);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StoreKindSpecifier other = (StoreKindSpecifier) obj;
		if (generation == null) {
			if (other.generation != null)
				return false;
		} else if (!generation.equals(other.generation))
			return false;
		if (kind == null) {
			if (other.kind != null)
				return false;
		} else if (!kind.equals(other.kind))
			return false;
		if (valueSpecifier == null) {
			if (other.valueSpecifier != null)
				return false;
		} else if (!valueSpecifier.equals(other.valueSpecifier))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "StoredDataSpecifier [kind=" + kind + ", generation=" + generation + ", valueSpecifier=" + valueSpecifier + "]";
	}

	static class StoreKindSpecifierCodec extends Codec<StoreKindSpecifier> {

		private static final int GENERATION_FIELD = U_INT64;
		private static final int VALUE_SPEC_LENGTH_FIELD = U_INT16;

		private final Codec<DataKind> kindCodec;

		public StoreKindSpecifierCodec(ComponentsContext ctx) {
			super(ctx);
			kindCodec = getCodec(DataKind.class);
		}

		@Override
		public void encode(StoreKindSpecifier obj, ByteBuf buf, Object... params) throws CodecException {
			kindCodec.encode(obj.kind, buf);

			byte[] genBytes = toUnsigned(obj.generation);

			// Make sure the field is of a fixed size by padding with zeros
			buf.writeZero(GENERATION_FIELD - genBytes.length);

			buf.writeBytes(genBytes);

			Field lenFld = allocateField(buf, VALUE_SPEC_LENGTH_FIELD);

			@SuppressWarnings("unchecked")
			Codec<ValueSpecifier> modelSpecCodec = (Codec<ValueSpecifier>) getCodec(obj.valueSpecifier.getClass());
			modelSpecCodec.encode(obj.valueSpecifier, buf);

			lenFld.updateDataLength();
		}

		@Override
		public StoreKindSpecifier decode(ByteBuf buf, Object... params) throws CodecException {
			DataKind kind = kindCodec.decode(buf);

			byte[] genData = new byte[GENERATION_FIELD];
			buf.readBytes(genData);
			BigInteger generation = new BigInteger(1, genData);

			ByteBuf valueSpecFld = readField(buf, VALUE_SPEC_LENGTH_FIELD);

			@SuppressWarnings("unchecked")
			Codec<ValueSpecifier> valueSpecCodec = (Codec<ValueSpecifier>) getCodec(kind.getDataModel().getSpecifierClass());

			ValueSpecifier valueSpec = valueSpecCodec.decode(valueSpecFld);

			StoreKindSpecifier spec = new StoreKindSpecifier();
			spec.setKind(kind);
			spec.setValueSpecifier(valueSpec);
			spec.setGeneration(generation);

			return spec;
		}

	}

}
