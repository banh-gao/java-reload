package com.github.reload.net.ice;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import dagger.ObjectGraph;
import com.github.reload.net.codecs.Codec;
import com.github.reload.net.codecs.Codec.ReloadCodec;
import com.github.reload.net.ice.IPv4AddressPort.IPv4AddresPortCodec;

@ReloadCodec(IPv4AddresPortCodec.class)
public class IPv4AddressPort extends IPAddressPort {

	public IPv4AddressPort(InetAddress addr, int port) {
		super(addr, port);
	}

	@Override
	protected AddressType getAddressType() {
		return AddressType.IPv4;
	}

	static class IPv4AddresPortCodec extends Codec<IPv4AddressPort> {

		private static final int ADDR_LENGTH = U_INT32;

		public IPv4AddresPortCodec(ObjectGraph ctx) {
			super(ctx);
		}

		@Override
		public void encode(IPv4AddressPort obj, ByteBuf buf, Object... params) throws com.github.reload.net.codecs.Codec.CodecException {
			buf.writeBytes(obj.getAddress().getAddress());
			buf.writeShort(obj.getPort());
		}

		@Override
		public IPv4AddressPort decode(ByteBuf buf, Object... params) throws com.github.reload.net.codecs.Codec.CodecException {
			return new IPv4AddressPort(decodeAddr(buf), decodePort(buf));
		}

		private InetAddress decodeAddr(ByteBuf buf) {
			byte[] tmpAddr = new byte[ADDR_LENGTH];
			buf.readBytes(tmpAddr);
			try {
				return InetAddress.getByAddress(tmpAddr);
			} catch (UnknownHostException e) {
				throw new DecoderException("Invalid IPv4 address");
			} catch (ClassCastException e) {
				throw new DecoderException("Invalid IPv4 address");
			}
		}

		public int decodePort(ByteBuf buf) {
			return buf.readUnsignedShort();
		}

	}
}
