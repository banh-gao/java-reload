package com.github.reload.storage.data;

import io.netty.buffer.ByteBuf;
import java.math.BigInteger;
import com.github.reload.Context;
import com.github.reload.net.data.Codec;
import com.github.reload.net.data.ReloadCodec;
import com.github.reload.storage.DataKind;
import com.github.reload.storage.data.DataModel.ModelSpecifier;
import com.github.reload.storage.data.StoredDataSpecifier.StoredDataSpecifierCodec;

@ReloadCodec(StoredDataSpecifierCodec.class)
public class StoredDataSpecifier {

	private final DataKind kind;
	private final BigInteger generation;
	private final ModelSpecifier modelSpecifier;

	StoredDataSpecifier(DataKind kind, BigInteger generation, ModelSpecifier spec) {
		this.kind = kind;
		this.generation = generation;
		modelSpecifier = spec;
	}

	public DataKind getDataKind() {
		return kind;
	}

	public ModelSpecifier getModelSpecifier() {
		return modelSpecifier;
	}

	public BigInteger getGeneration() {
		return generation;
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
			Codec<ModelSpecifier> modelSpecCodec = (Codec<ModelSpecifier>) getCodec(obj.modelSpecifier.getClass());
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
			Codec<ModelSpecifier> modelSpecCodec = (Codec<ModelSpecifier>) getCodec(kind.getDataModel().getSpecifierClass());

			ModelSpecifier modelSpec = modelSpecCodec.decode(modelSpecFld);

			return new StoredDataSpecifier(kind, generation, modelSpec);
		}

	}

}
