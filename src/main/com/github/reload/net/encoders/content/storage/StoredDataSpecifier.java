package com.github.reload.net.encoders.content.storage;

import io.netty.buffer.ByteBuf;
import java.math.BigInteger;
import java.util.Objects;
import com.github.reload.conf.Configuration;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.encoders.content.storage.StoredDataSpecifier.StoredDataSpecifierCodec;
import com.github.reload.storage.DataKind;
import com.github.reload.storage.DataModel.DataValue;
import com.github.reload.storage.DataModel.ModelSpecifier;

@ReloadCodec(StoredDataSpecifierCodec.class)
public class StoredDataSpecifier {

	private final DataKind kind;
	private BigInteger generation = BigInteger.ZERO;
	private final ModelSpecifier<? extends DataValue> modelSpecifier;

	public StoredDataSpecifier(DataKind kind, ModelSpecifier<? extends DataValue> spec) {
		this.kind = kind;
		modelSpecifier = spec;
	}

	public DataKind getKind() {
		return kind;
	}

	public ModelSpecifier<? extends DataValue> getModelSpecifier() {
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
		StoredDataSpecifier other = (StoredDataSpecifier) obj;
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

	static class StoredDataSpecifierCodec extends Codec<StoredDataSpecifier> {

		private static final int GENERATION_FIELD = U_INT64;
		private static final int MODEL_SPEC_LENGTH_FIELD = U_INT16;

		private final Codec<DataKind> kindCodec;

		public StoredDataSpecifierCodec(Configuration conf) {
			super(conf);
			kindCodec = getCodec(DataKind.class);
		}

		@Override
		public void encode(StoredDataSpecifier obj, ByteBuf buf, Object... params) throws CodecException {
			kindCodec.encode(obj.kind, buf);

			byte[] genBytes = obj.generation.toByteArray();

			// Make sure the field is of a fixed size by padding with zeros
			buf.writeZero(GENERATION_FIELD - genBytes.length);

			buf.writeBytes(genBytes);

			Field lenFld = allocateField(buf, MODEL_SPEC_LENGTH_FIELD);

			@SuppressWarnings("unchecked")
			Codec<ModelSpecifier<? extends DataValue>> modelSpecCodec = (Codec<ModelSpecifier<? extends DataValue>>) getCodec(obj.modelSpecifier.getClass());
			modelSpecCodec.encode(obj.modelSpecifier, buf);

			lenFld.updateDataLength();
		}

		@Override
		public StoredDataSpecifier decode(ByteBuf buf, Object... params) throws CodecException {
			DataKind kind = kindCodec.decode(buf);

			byte[] genData = new byte[GENERATION_FIELD];
			buf.readBytes(genData);
			BigInteger generation = new BigInteger(1, genData);

			ByteBuf modelSpecFld = readField(buf, MODEL_SPEC_LENGTH_FIELD);

			@SuppressWarnings("unchecked")
			Codec<ModelSpecifier<? extends DataValue>> modelSpecCodec = (Codec<ModelSpecifier<? extends DataValue>>) getCodec(kind.getDataModel().getSpecifierClass());

			ModelSpecifier<? extends DataValue> modelSpec = modelSpecCodec.decode(modelSpecFld);

			StoredDataSpecifier spec = new StoredDataSpecifier(kind, modelSpec);
			spec.setGeneration(generation);

			return spec;
		}

	}

}
