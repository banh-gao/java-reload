package com.github.reload.net.ice;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import com.github.reload.Configuration;
import com.github.reload.net.data.Codec;

public class IPv6AddressPort extends IPAddressPort {

	public IPv6AddressPort(InetAddress addr, int port) {
		super(addr, port);
	}

	@Override
	protected AddressType getAddressType() {
		return AddressType.IPv6;
	}

	public static class IPv6AddressPortCodec extends Codec<IPv6AddressPort> {

		private static final int ADDR_LENGTH = U_INT128;

		public IPv6AddressPortCodec(Configuration conf) {
			super(conf);
		}

		@Override
		public void encode(IPv6AddressPort obj, ByteBuf buf, Object... params) throws com.github.reload.net.data.Codec.CodecException {
			buf.writeBytes(obj.getAddress().getAddress());
			buf.writeShort(obj.getPort());
		}

		@Override
		public IPv6AddressPort decode(ByteBuf buf, Object... params) throws com.github.reload.net.data.Codec.CodecException {
			return new IPv6AddressPort(parseAddr(buf), parsePort(buf));
		}

		private InetAddress parseAddr(ByteBuf buf) {
			byte[] tmpAddr = new byte[ADDR_LENGTH];
			buf.readBytes(tmpAddr);
			try {
				return InetAddress.getByAddress(tmpAddr);
			} catch (UnknownHostException e) {
				throw new DecoderException("Invalid IPv6 address");
			} catch (ClassCastException e) {
				throw new DecoderException("Invalid IPv6 address");
			}

		}

		private int parsePort(ByteBuf buf) {
			return buf.readUnsignedShort();
		}

	}
}
