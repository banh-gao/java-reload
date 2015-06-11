package com.github.reload.services.storage.encoders;

import io.netty.buffer.ByteBuf;
import java.math.BigInteger;
import java.util.Objects;
import com.github.reload.components.ComponentsContext;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.services.storage.DataKind;
import com.github.reload.services.storage.encoders.DataModel.ValueSpecifier;
import com.github.reload.services.storage.encoders.StoreKindDataSpecifier.StoreKindDataSpecifierCodec;

@ReloadCodec(StoreKindDataSpecifierCodec.class)
public class StoreKindDataSpecifier {

	private final DataKind kind;
	private BigInteger generation = BigInteger.ZERO;
	private final ValueSpecifier modelSpecifier;

	public StoreKindDataSpecifier(DataKind kind) {
		this(kind, kind.getDataModel().newSpecifier());
	}

	StoreKindDataSpecifier(DataKind kind, ValueSpecifier modelSpecifier) {
		this.kind = kind;
		this.modelSpecifier = modelSpecifier;
	}

	public DataKind getKind() {
		return kind;
	}

	public ValueSpecifier getValueSpecifier() {
		return modelSpecifier;
	}

	public BigInteger getGeneration() {
		return generation;
	}

	public void setGeneration(BigInteger generation) {
		this.generation = generation;
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), kind, generation, modelSpecifier);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StoreKindDataSpecifier other = (StoreKindDataSpecifier) obj;
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
		if (modelSpecifier == null) {
			if (other.modelSpecifier != null)
				return false;
		} else if (!modelSpecifier.equals(other.modelSpecifier))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "StoredDataSpecifier [kind=" + kind + ", generation=" + generation + ", modelSpecifier=" + modelSpecifier + "]";
	}

	static class StoreKindDataSpecifierCodec extends Codec<StoreKindDataSpecifier> {

		private static final int GENERATION_FIELD = U_INT64;
		private static final int MODEL_SPEC_LENGTH_FIELD = U_INT16;

		private final Codec<DataKind> kindCodec;

		public StoreKindDataSpecifierCodec(ComponentsContext ctx) {
			super(ctx);
			kindCodec = getCodec(DataKind.class);
		}

		@Override
		public void encode(StoreKindDataSpecifier obj, ByteBuf buf, Object... params) throws CodecException {
			kindCodec.encode(obj.kind, buf);

			byte[] genBytes = toUnsigned(obj.generation);

			// Make sure the field is of a fixed size by padding with zeros
			buf.writeZero(GENERATION_FIELD - genBytes.length);

			buf.writeBytes(genBytes);

			Field lenFld = allocateField(buf, MODEL_SPEC_LENGTH_FIELD);

			@SuppressWarnings("unchecked")
			Codec<ValueSpecifier> modelSpecCodec = (Codec<ValueSpecifier>) getCodec(obj.modelSpecifier.getClass());
			modelSpecCodec.encode(obj.modelSpecifier, buf);

			lenFld.updateDataLength();
		}

		@Override
		public StoreKindDataSpecifier decode(ByteBuf buf, Object... params) throws CodecException {
			DataKind kind = kindCodec.decode(buf);

			byte[] genData = new byte[GENERATION_FIELD];
			buf.readBytes(genData);
			BigInteger generation = new BigInteger(1, genData);

			ByteBuf modelSpecFld = readField(buf, MODEL_SPEC_LENGTH_FIELD);

			@SuppressWarnings("unchecked")
			Codec<ValueSpecifier> modelSpecCodec = (Codec<ValueSpecifier>) getCodec(kind.getDataModel().getSpecifierClass());

			ValueSpecifier modelSpec = modelSpecCodec.decode(modelSpecFld);

			StoreKindDataSpecifier spec = new StoreKindDataSpecifier(kind, modelSpec);
			spec.setGeneration(generation);

			return spec;
		}

	}

}
