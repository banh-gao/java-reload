package com.github.reload.net.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.GeneralSecurityException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import com.github.reload.Configuration;
import com.github.reload.Components;
import com.github.reload.net.encoders.content.Content;
import com.github.reload.net.encoders.content.PingAnswer;
import com.github.reload.net.encoders.header.Header;
import com.github.reload.net.encoding.Message;
import com.google.common.util.concurrent.AbstractExecutionThreadService;

/**
 * Responder server that send a ping answer to peers searching for a
 * bootstrap server in a multicast group
 */
public class DiscoveryResponder extends AbstractExecutionThreadService {

	private static final Logger l = Logger.getRootLogger();

	private final Components context;
	private final InetSocketAddress localAddr;
	private final EventLoopGroup handlersLoopGroup = new NioEventLoopGroup();

	private Channel channel;

	public DiscoveryResponder(Components context, InetSocketAddress localAddr) {
		this.context = context;
		this.localAddr = localAddr;
	}

	@Override
	protected void startUp() throws Exception {
		ServerBootstrap sb = new ServerBootstrap();
		// TODO: initialize multicast server
		// ChannelHandler chHandler = new ChannelInitializerImpl(context,
		// linkType);
		sb.group(handlersLoopGroup).channel(NioServerSocketChannel.class);
		sb.childHandler(new ResponderChannelInit());
		sb.childOption(ChannelOption.SO_KEEPALIVE, true);
		ChannelFuture f = sb.bind(localAddr).await();
		this.channel = f.channel();
	}

	@Override
	protected void shutDown() throws Exception {
		handlersLoopGroup.shutdownGracefully().await();
		l.log(Level.DEBUG, "Multicast discovery responder stopped.");
	}

	private void sendAnswer(DatagramPacket packet) throws Exception {
		UnsignedByteBuffer buf = UnsignedByteBuffer.wrap(packet.getData(), 0, packet.getLength());
		Header header = new Header(buf, null);

		buf.limit(header.getMessageLength());

		Message requestMessage = Message.getDecoded(context, header, buf);

		if (!isValidRequest(requestMessage, packet.getSocketAddress()))
			throw new Exception("Invalid discovery request from " + requestMessage.getSenderId() + " at " + packet.getSocketAddress());

		Content answerContent;

		answerContent = new PingAnswer();

		sendMessage((InetSocketAddress) packet.getSocketAddress(), header, answerContent);
		l.log(Level.DEBUG, "Sent discovery answer to " + requestMessage.getSenderId() + " at " + packet.getSocketAddress());
	}

	private void sendMessage(InetSocketAddress sourceAddress, ForwardingHeader requestHeader, Content answerContent) {
		Message response = context.getMessageBuilder().newResponseMessage(requestHeader, answerContent);
		txMessageBuf.clear();
		response.writeTo(txMessageBuf, context);

		channel.write(response);
		try {
			udpSocket.send(new DatagramPacket(txMessageBuf.array(), txMessageBuf.position(), sourceAddress));
		} catch (IOException e) {
			l.log(Level.WARN, e.getMessage(), e);
			return;
		}
	}

	private boolean isValidRequest(Message request, SocketAddress addr) {
		String hash = context.getComponent(Configuration.class).getOverlayHash();
		if (!hash.equals(request.getHeader().getOverlayHash()))
			return false;

		try {
			request.verify();
			return true;
		} catch (GeneralSecurityException e) {
			l.log(Level.DEBUG, "Authentication failed for discovery request from untrusted sender " + request.getHeader().getSenderId() + " at " + addr, e);
			return false;
		}
	}

	public InetSocketAddress getLocalSocketAddress() {
		return localAddr;
	}

	@Override
	protected String serviceName() {
		return "Bootstrap Discovery Responder";
	}

	private class ResponderChannelInit extends ChannelInitializer<Channel> {

		@Override
		protected void initChannel(Channel ch) throws Exception {
			// TODO Auto-generated method stub

		}

	}

	private class MessageHandler extends ChannelDuplexHandler {

	}
}
