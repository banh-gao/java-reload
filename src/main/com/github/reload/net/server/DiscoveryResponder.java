package com.github.reload.net.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import com.github.reload.net.encoders.Message;
import com.github.reload.net.encoders.MessageBuilder;
import com.github.reload.net.encoders.content.PingAnswer;
import com.github.reload.net.ice.HostCandidate.OverlayLinkType;
import com.github.reload.net.stack.ReloadStack;

/**
 * Responder server that send a ping answer to peers searching for a
 * bootstrap server in a multicast group
 */
public class DiscoveryResponder {

	private static final Logger l = Logger.getRootLogger();

	private final ReloadStack stack;
	private final MessageBuilder builder;

	public DiscoveryResponder(OverlayLinkType linkType, InetSocketAddress localAddr, MessageBuilder builder) throws NoSuchAlgorithmException, InterruptedException {
		UDPReloadStackBuilder b = UDPReloadStackBuilder.newServerBuilder(new MessageHandler());
		b.setLinkType(linkType);
		b.setLocalAddress(localAddr);
		stack = b.buildStack();
		this.builder = builder;
	}

	protected void shutDown() throws Exception {
		stack.shutdown().sync();
		l.log(Level.DEBUG, "Multicast discovery responder stopped.");
	}

	public InetSocketAddress getLocalSocketAddress() {
		return (InetSocketAddress) stack.getChannel().localAddress();
	}

	private class MessageHandler extends ChannelInboundHandlerAdapter {

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			Message req = (Message) msg;

			Message response = builder.newResponseMessage(req.getHeader(), new PingAnswer(0, BigInteger.valueOf(System.currentTimeMillis())));

			ctx.channel().write(response);

			l.debug("Sent discovery answer to " + req.getHeader().getSenderId() + " at " + ctx.channel().remoteAddress());
		}
	}
}
