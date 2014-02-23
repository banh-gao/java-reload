package com.github.reload.storage.data;

import io.netty.buffer.ByteBuf;
import java.math.BigInteger;
import com.github.reload.Context;
import com.github.reload.DataKind;
import com.github.reload.net.data.Codec;
import com.github.reload.net.data.ReloadCodec;
import com.github.reload.storage.data.DataModel.DataValue;
import com.github.reload.storage.data.DataModel.ModelSpecifier;
import com.github.reload.storage.data.StoredDataSpecifier.StoredDataSpecifierCodec;

@ReloadCodec(StoredDataSpecifierCodec.class)
public class StoredDataSpecifier {

	private final DataKind kind;
	private BigInteger generation;
	private final ModelSpecifier<? extends DataValue> modelSpecifier;

	public StoredDataSpecifier(DataKind kind, ModelSpecifier<? extends DataValue> spec) {
		this.kind = kind;
		modelSpecifier = spec;
	}

	public DataKind getDataKind() {
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

	public static class StoredDataSpecifierCodec extends Codec<StoredDataSpecifier> {

		private static final int MODEL_SPEC_LENGTH_FIELD = U_INT16;

		private final Codec<DataKind> kindCodec;

		public StoredDataSpecifierCodec(Context context) {
			super(context);
			kindCodec = getCodec(DataKind.class);
		}

		@Override
		public void encode(StoredDataSpecifier obj, ByteBuf buf, Object... params) throws CodecException {
			kindCodec.encode(obj.kind, buf);

			buf.writeBytes(obj.generation.toByteArray());

			Field lenFld = allocateField(buf, MODEL_SPEC_LENGTH_FIELD);

			@SuppressWarnings("unchecked")
			Codec<ModelSpecifier<? extends DataValue>> modelSpecCodec = (Codec<ModelSpecifier<? extends DataValue>>) getCodec(obj.modelSpecifier.getClass());
			modelSpecCodec.encode(obj.modelSpecifier, buf);

			lenFld.updateDataLength();
		}

		@Override
		public StoredDataSpecifier decode(ByteBuf buf, Object... params) throws CodecException {
			DataKind kind = kindCodec.decode(buf);

			byte[] genData = new byte[8];
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
