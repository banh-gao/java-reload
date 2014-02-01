package com.github.reload.net.data;

import java.util.List;
import com.github.reload.message.Header;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;


public class OpaqueMessageCodec extends ByteToMessageCodec<Header> {

	@Override
	protected void encode(ChannelHandlerContext ctx, Header msg, ByteBuf out) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		// TODO Auto-generated method stub

	}

}
