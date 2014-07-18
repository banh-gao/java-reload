package com.github.reload.net.ice;

import io.netty.buffer.ByteBuf;
import com.github.reload.Configuration;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.ice.IPAddressPort.AddressType;

class IPAddressPortCodec extends Codec<IPAddressPort> {

	public static final int ADDR_LENGTH_FIELD = U_INT8;

	private final Codec<IPv4AddressPort> ip4codec;
	private final Codec<IPv6AddressPort> ip6codec;

	public IPAddressPortCodec(Configuration conf) {
		super(conf);
		ip4codec = getCodec(IPv4AddressPort.class);
		ip6codec = getCodec(IPv6AddressPort.class);
	}

	@Override
	public void encode(IPAddressPort obj, ByteBuf buf, Object... params) throws com.github.reload.net.encoders.Codec.CodecException {
		buf.writeByte(obj.getAddressType().code);
		Field lenFld = allocateField(buf, ADDR_LENGTH_FIELD);

		switch (obj.getAddressType()) {
			case IPv4 :
				ip4codec.encode((IPv4AddressPort) obj, buf);
				break;
			case IPv6 :
				ip6codec.encode((IPv6AddressPort) obj, buf);
			default :
				break;
		}

		lenFld.updateDataLength();
	}

	@Override
	public IPAddressPort decode(ByteBuf buf, Object... params) throws CodecException {
		AddressType type = AddressType.valueOf(buf.readByte());
		if (type == null)
			throw new CodecException("Unknown address type");

		IPAddressPort out = null;

		ByteBuf dataBuf = readField(buf, ADDR_LENGTH_FIELD);

		switch (type) {
			case IPv4 :
				out = ip4codec.decode(dataBuf);
				break;
			case IPv6 :
				out = ip6codec.decode(dataBuf);
				break;
		}

		return out;
	}

}
