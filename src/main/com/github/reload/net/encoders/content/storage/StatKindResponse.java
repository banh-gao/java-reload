package com.github.reload.net.encoders.content.storage;

import io.netty.buffer.ByteBuf;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import com.github.reload.Configuration;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.encoders.content.storage.StatKindResponse.StatKindResponseCodec;
import com.github.reload.storage.DataKind;
import com.github.reload.storage.DataModel;
import com.github.reload.storage.DataModel.DataValue;

/**
 * A response contained in a stat answer, contains all the data for a specific
 * kind that matches the request specifier
 * 
 */
@ReloadCodec(StatKindResponseCodec.class)
public class StatKindResponse {

	private final DataKind kind;
	private final BigInteger generation;
	private final List<StoredMetadata> values;

	public StatKindResponse(DataKind kind, BigInteger generation, List<StoredMetadata> values) {
		this.kind = kind;
		this.generation = generation;
		this.values = values;
	}

	public DataKind getKind() {
		return kind;
	}

	public BigInteger getGeneration() {
		return generation;
	}

	public List<StoredMetadata> getValues() {
		return values;
	}

	public static class StatKindResponseCodec extends Codec<StatKindResponse> {

		private final static int VALUES_LENGTH_FIELD = U_INT32;

		private final Codec<DataKind> dataKindCodec;
		private final Codec<StoredMetadata> storedDataCodec;

		public StatKindResponseCodec(Configuration conf) {
			super(conf);
			dataKindCodec = getCodec(DataKind.class);
			storedDataCodec = getCodec(StoredMetadata.class);
		}

		@Override
		public void encode(StatKindResponse obj, ByteBuf buf, Object... params) throws com.github.reload.net.encoders.Codec.CodecException {
			dataKindCodec.encode(obj.kind, buf);
			buf.writeBytes(obj.generation.toByteArray());
			encodeData(obj, buf);
		}

		private void encodeData(StatKindResponse obj, ByteBuf buf) throws com.github.reload.net.encoders.Codec.CodecException {
			Field dataFld = allocateField(buf, VALUES_LENGTH_FIELD);
			for (StoredMetadata d : obj.values) {
				storedDataCodec.encode(d, buf);
			}
			dataFld.updateDataLength();
		}

		@Override
		public StatKindResponse decode(ByteBuf buf, Object... params) throws com.github.reload.net.encoders.Codec.CodecException {
			DataKind kind = dataKindCodec.decode(buf);

			byte[] genData = new byte[8];
			buf.readBytes(genData);
			BigInteger generation = new BigInteger(1, genData);

			List<StoredMetadata> values = decodeData(kind, buf);
			return new StatKindResponse(kind, generation, values);
		}

		private List<StoredMetadata> decodeData(DataKind kind, ByteBuf buf) throws CodecException {
			ByteBuf respData = readField(buf, VALUES_LENGTH_FIELD);

			List<StoredMetadata> out = new ArrayList<StoredMetadata>();

			DataModel<? extends DataValue> dataModel = kind.getDataModel();

			while (respData.readableBytes() > 0) {
				StoredMetadata data = storedDataCodec.decode(respData, dataModel);
				out.add(data);
			}
			respData.release();

			return out;
		}

	}
}