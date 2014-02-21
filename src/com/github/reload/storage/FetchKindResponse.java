package com.github.reload.storage;

import io.netty.buffer.ByteBuf;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import com.github.reload.Context;
import com.github.reload.net.data.Codec;
import com.github.reload.net.data.ReloadCodec;
import com.github.reload.storage.FetchKindResponse.FetchKindResponseCodec;
import com.github.reload.storage.data.StoredData;

/**
 * A response contained in a fetch answer, contains all the data for a specific
 * kind that matches the request data specifier
 * 
 */
@ReloadCodec(FetchKindResponseCodec.class)
public class FetchKindResponse {

	private final DataKind kind;
	private final BigInteger generation;
	private final List<StoredData> values;

	public FetchKindResponse(DataKind kind, BigInteger generation, List<StoredData> values) {
		super();
		this.kind = kind;
		this.generation = generation;
		this.values = values;
	}

	public static class FetchKindResponseCodec extends Codec<FetchKindResponse> {

		private final static int VALUES_LENGTH_FIELD = U_INT32;

		private final Codec<DataKind> dataKindCodec;
		private final Codec<StoredData> storedDataCodec;

		public FetchKindResponseCodec(Context context) {
			super(context);
			dataKindCodec = getCodec(DataKind.class);
			storedDataCodec = getCodec(StoredData.class);
		}

		@Override
		public void encode(FetchKindResponse obj, ByteBuf buf) throws CodecException {
			// TODO Auto-generated method stub

		}

		@Override
		public FetchKindResponse decode(ByteBuf buf) throws CodecException {
			DataKind kind = dataKindCodec.decode(buf);

			byte[] genData = new byte[8];
			buf.readBytes(genData);
			BigInteger generation = new BigInteger(1, genData);

			List<StoredData> values = decodeData(kind, buf);
			return new FetchKindResponse(kind, generation, values);
		}

		private List<StoredData> decodeData(DataKind kind, ByteBuf buf) throws CodecException {
			ByteBuf respData = readField(buf, VALUES_LENGTH_FIELD);

			List<ResponseData> out = new ArrayList<ResponseData>();

			while (respData.readableBytes() > 0) {
				ResponseData data = decodeData(respData);
				out.add(data);
			}
			respData.release();

			return out;
		}

	}
}
