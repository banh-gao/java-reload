package com.github.reload.storage;

import io.netty.buffer.ByteBuf;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import com.github.reload.Context;
import com.github.reload.net.data.Codec;

public class KindResponse implements Iterable<ResponseData> {

	private final DataKind kind;
	private final BigInteger generation;
	private final List<ResponseData> values;

	public KindResponse(DataKind kind, BigInteger generation, List<ResponseData> values) {
		this.kind = kind;
		this.generation = generation;
		this.values = values;
	}

	public abstract static class KindResponseCodec extends Codec<KindResponse> {

		private final static int VALUES_LENGTH_FIELD = U_INT32;

		private final Codec<DataKind> dataKindCodec;

		public KindResponseCodec(Context context) {
			super(context);
			dataKindCodec = getCodec(DataKind.class);
		}

		@Override
		public KindResponse decode(ByteBuf buf) throws CodecException {
			DataKind kind = dataKindCodec.decode(buf);

			byte[] genData = new byte[8];
			buf.readBytes(genData);
			BigInteger generation = new BigInteger(1, genData);

			List<ResponseData> values = decodedValues(kind, buf);

			return new KindResponse(kind, generation, values);
		}

		private List<ResponseData> decodedValues(DataKind kind, ByteBuf buf) throws CodecException {
			ByteBuf respData = readField(buf, VALUES_LENGTH_FIELD);

			List<ResponseData> out = new ArrayList<ResponseData>();

			while (respData.readableBytes() > 0) {
				ResponseData data = decodeData(respData);
				out.add(data);
			}
			respData.release();

			return out;
		}

		protected abstract ResponseData decodeData(ByteBuf buf) throws CodecException;

		@Override
		public void encode(KindResponse obj, ByteBuf buf) throws com.github.reload.net.data.Codec.CodecException {
			dataKindCodec.encode(obj.kind, buf);
			buf.writeBytes(obj.generation.toByteArray());

			Field dataFld = allocateField(buf, VALUES_LENGTH_FIELD);

			for (ResponseData data : obj.values) {
				encodeData(data, buf);
			}

			dataFld.updateDataLength();
		}

		protected abstract void encodeData(ResponseData data, ByteBuf buf) throws CodecException;
	}

	public BigInteger getGenerationCounter() {
		return generation;
	}

	public DataKind getKind() {
		return kind;
	}

	public List<ResponseData> getValues() {
		return values;
	}

	@Override
	public Iterator<ResponseData> iterator() {
		return values.iterator();
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " [kind=" + kind.getKindId() + ", generation=" + generation + ", values=" + values + "]";
	}
}
