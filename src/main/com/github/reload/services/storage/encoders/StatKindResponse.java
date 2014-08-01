package com.github.reload.services.storage.encoders;

import io.netty.buffer.ByteBuf;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import com.github.reload.conf.Configuration;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.services.storage.DataKind;
import com.github.reload.services.storage.DataModel;
import com.github.reload.services.storage.DataModel.DataValue;
import com.github.reload.services.storage.encoders.StatKindResponse.StatKindResponseCodec;

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

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), kind, generation, values);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StatKindResponse other = (StatKindResponse) obj;
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
		if (values == null) {
			if (other.values != null)
				return false;
		} else if (!values.equals(other.values))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "StatKindResponse [kind=" + kind + ", generation=" + generation + ", values=" + values + "]";
	}

	static class StatKindResponseCodec extends Codec<StatKindResponse> {

		private final static int GENERATION_FIELD = U_INT64;
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

			byte[] genBytes = obj.generation.toByteArray();

			// Make sure the field has a fixed size by padding with zeros
			buf.writeZero(GENERATION_FIELD - genBytes.length);
			buf.writeBytes(genBytes);

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

			byte[] genData = new byte[GENERATION_FIELD];
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