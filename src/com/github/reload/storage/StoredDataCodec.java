package com.github.reload.storage;

import io.netty.buffer.ByteBuf;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import com.github.reload.Context;
import com.github.reload.message.GenericSignature;
import com.github.reload.net.data.Codec;
import com.github.reload.storage.ResponseData.ResponseDataCodec;
import com.github.reload.storage.data.DataValue;
import com.github.reload.storage.data.StoredData;

public class StoredDataCodec extends ResponseDataCodec {

	public StoredDataCodec(Context context) {
		super(context);
	}

	@Override
	public void encodeData(StoredData obj, ByteBuf buf) throws CodecException {

		Codec<DataValue> valueCodec = (Codec<DataValue>) getCodec(obj.getValue().getClass());
		valueCodec.encode(obj.getValue(), buf);

		Codec<GenericSignature> signatureCodec = getCodec(GenericSignature.class);
		signatureCodec.encode(obj.getSignature(), buf);
	}

	@Override
	public StoredData decode(ByteBuf buf, Map<String,Object> params) throws com.github.reload.net.data.Codec.CodecException {
		ByteBuf dataFld = readField(buf, DATA_LENGTH_FIELD);

		byte[] storageTimeData = new byte[8];
		dataFld.readBytes(storageTimeData);
		BigInteger storageTime = new BigInteger(1, storageTimeData);
		
		long lifeTime = dataFld.readUnsignedInt();

		
		value = kind.parseValue(buf, valueLength);

		try {
			signature = GenericSignature.parse(buf);
		} catch (NoSuchAlgorithmException e) {
			throw new DecodingException("Unsupported algorithm: " + e.getMessage());
		}
		new StoredData(storageTime, lifetime, value, signature)
	}
}
