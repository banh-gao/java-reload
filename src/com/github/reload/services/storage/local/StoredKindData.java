package com.github.reload.services.storage.local;

import io.netty.buffer.ByteBuf;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import dagger.ObjectGraph;
import com.github.reload.net.codecs.Codec;
import com.github.reload.net.codecs.Codec.ReloadCodec;
import com.github.reload.services.storage.DataKind;
import com.github.reload.services.storage.local.StoredKindData.StoreKindDataCodec;

@ReloadCodec(StoreKindDataCodec.class)
public class StoredKindData {

	protected final DataKind kind;

	BigInteger generation;

	private final List<StoredData> data;

	public StoredKindData(DataKind kind, BigInteger generation, List<StoredData> data) {
		this.kind = kind;
		this.generation = generation;
		this.data = data;
	}

	public DataKind getKind() {
		return kind;
	}

	public BigInteger getGeneration() {
		return generation;
	}

	public List<StoredData> getValues() {
		return data;
	}

	static class StoreKindDataCodec extends Codec<StoredKindData> {

		protected static final int GENERATION_FIELD = U_INT64;
		protected static final int VALUES_LENGTH_FIELD = U_INT32;

		private final Codec<DataKind> kindCodec;
		private final Codec<StoredData> dataCodec;

		public StoreKindDataCodec(ObjectGraph ctx) {
			super(ctx);
			kindCodec = getCodec(DataKind.class);
			dataCodec = getCodec(StoredData.class);
		}

		@Override
		public void encode(StoredKindData obj, ByteBuf buf, Object... params) throws com.github.reload.net.codecs.Codec.CodecException {
			kindCodec.encode(obj.kind, buf);

			byte[] generationBytes = toUnsigned(obj.generation);
			// Make sure generation field is always of the fixed size by
			// padding with zeros
			buf.writeZero(GENERATION_FIELD - generationBytes.length);

			buf.writeBytes(generationBytes);

			Field lenFld = allocateField(buf, VALUES_LENGTH_FIELD);

			for (StoredData d : obj.data) {
				dataCodec.encode(d, buf);
			}

			lenFld.updateDataLength();
		}

		@Override
		public StoredKindData decode(ByteBuf buf, Object... params) throws com.github.reload.net.codecs.Codec.CodecException {
			DataKind kind = kindCodec.decode(buf);

			byte[] generationData = new byte[GENERATION_FIELD];
			buf.readBytes(generationData);
			BigInteger generation = new BigInteger(1, generationData);

			List<StoredData> data = decodeStoredDataList(kind, buf);
			return new StoredKindData(kind, generation, data);
		}

		private List<StoredData> decodeStoredDataList(DataKind kind, ByteBuf buf) throws com.github.reload.net.codecs.Codec.CodecException {
			ByteBuf dataFld = readField(buf, VALUES_LENGTH_FIELD);

			List<StoredData> data = new ArrayList<StoredData>();

			while (dataFld.readableBytes() > 0) {
				data.add(dataCodec.decode(dataFld, kind.getDataModel()));
			}

			return data;
		}

	}
}
