package com.github.reload.net.connections;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import java.net.InetSocketAddress;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Map;
import org.apache.log4j.Logger;
import com.github.reload.Bootstrap;
import com.github.reload.Components;
import com.github.reload.Components.Component;
import com.github.reload.Components.start;
import com.github.reload.Components.stop;
import com.github.reload.conf.Configuration;
import com.github.reload.crypto.CryptoHelper;
import com.github.reload.crypto.ReloadCertificate;
import com.github.reload.net.encoders.Message;
import com.github.reload.net.encoders.MessageBuilder;
import com.github.reload.net.encoders.content.LeaveRequest;
import com.github.reload.net.encoders.header.DestinationList;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.ice.HostCandidate.OverlayLinkType;
import com.github.reload.net.stack.MessageDispatcher;
import com.github.reload.net.stack.ReloadStack;
import com.github.reload.net.stack.ReloadStackBuilder;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

/**
 * Establish and manage connections for all neighbor nodes
 */
@Component(ConnectionManager.COMPNAME)
public class ConnectionManager {

	public static final String COMPNAME = "com.github.reload.net.connections.ConnectionManager";

	private static final Logger l = Logger.getRootLogger();
	private static final OverlayLinkType SERVER_PROTO = OverlayLinkType.TLS_TCP_FH_NO_ICE;

	@Component
	private CryptoHelper<Certificate> cryptoHelper;

	@Component
	private Configuration conf;

	@Component
	private Bootstrap connector;

	@Component
	private MessageBuilder msgBuilder;

	private final Map<NodeID, Connection> connections = Maps.newHashMap();

	private final MessageDispatcher msgDispatcher;
	private final ServerStatusHandler serverStatusHandler;

	private ReloadStack attachServer;

	public ConnectionManager() {
		msgDispatcher = new MessageDispatcher();
		serverStatusHandler = new ServerStatusHandler(this);
	}

	@start
	private void startServer() throws Exception {
		ReloadStackBuilder b = ReloadStackBuilder.newServerBuilder(msgDispatcher, serverStatusHandler);
		b.setLocalAddress(connector.getLocalAddr());
		b.setLinkType(SERVER_PROTO);
		attachServer = b.buildStack();
		l.info(String.format("Server started at %s", attachServer.getChannel().localAddress()));
	}

	@stop
	private void shutdown() {
		attachServer.shutdown();
		for (Connection c : connections.values()) {
			sendLeaveAndClose(c);
		}
	}

	private void sendLeaveAndClose(final Connection c) {
		// TODO: get leaving data from topology plugin
		Message m = msgBuilder.newMessage(new LeaveRequest(connector.getLocalNodeId(), new byte[0]), new DestinationList(c.getNodeId()));
		ChannelFuture cf = c.write(m);
		cf.addListener(new ChannelFutureListener() {

			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				c.close();
			}
		});
	}

	public ListenableFuture<Connection> connectTo(final InetSocketAddress remoteAddr, OverlayLinkType linkType) {
		final ReloadStack stack;

		final SettableFuture<Connection> outcome = SettableFuture.create();

		try {
			ReloadStackBuilder b = ReloadStackBuilder.newClientBuilder(msgDispatcher);
			b.setLinkType(linkType);
			stack = b.buildStack();
		} catch (Exception e) {
			outcome.setException(e);
			return outcome;
		}

		l.debug("Connecting to " + remoteAddr + " ...");

		ChannelFuture cf = stack.connect(remoteAddr);

		cf.addListener(new ChannelFutureListener() {

			@Override
			public void operationComplete(final ChannelFuture future) throws Exception {

				if (future.isSuccess()) {
					ChannelPipeline pipeline = future.channel().pipeline();
					SslHandler sslHandler = (SslHandler) pipeline.get(ReloadStack.HANDLER_SSL);
					Future<Channel> handshakeFuture = sslHandler.handshakeFuture();
					handshakeFuture.addListener(new FutureListener<Channel>() {

						public void operationComplete(Future<Channel> future) throws Exception {

							Components.getComponentsExecutor().execute(new Runnable() {

								@Override
								public void run() {
									Connection c;
									try {
										c = addConnection(stack);
									} catch (CertificateException e) {
										l.debug("Connection to " + remoteAddr + " terminated: Invalid RELOAD certificate", e);
										outcome.setException(e);
										return;
									}

									l.debug("Connection to " + c.getNodeId() + " at " + remoteAddr + " completed");
									outcome.set(c);
								}
							});
						};
					});
				} else {
					l.warn("Connection to " + remoteAddr + " failed", future.cause());
					outcome.setException(future.cause());
				}

			}
		});

		return outcome;
	}

	void clientConnected(final Channel channel) {
		SslHandler sslHandler = (SslHandler) channel.pipeline().get(ReloadStack.HANDLER_SSL);
		Future<Channel> handshakeFuture = sslHandler.handshakeFuture();

		handshakeFuture.addListener(new FutureListener<Channel>() {

			public void operationComplete(final Future<Channel> future) throws Exception {
				Components.getComponentsExecutor().execute(new Runnable() {

					@Override
					public void run() {
						if (!future.isSuccess())
							future.cause().printStackTrace();
						try {
							Connection c = addConnection(new ReloadStack(channel));
							l.debug(String.format("Connection from node %s at %s accepted", c.getNodeId(), channel.remoteAddress()));
						} catch (CertificateException e) {
							l.debug(String.format("Connection from %s rejected: Invalid RELOAD certificate", channel.remoteAddress()), e);
							return;
						}
					}
				});
			};
		});
	}

	private Connection addConnection(ReloadStack stack) throws CertificateException {
		ReloadCertificate cert = extractRemoteCert(stack.getChannel());
		cryptoHelper.addCertificate(cert);
		Connection c = new Connection(cert.getNodeId(), stack);

		connections.put(cert.getNodeId(), c);
		return c;
	}

	private ReloadCertificate extractRemoteCert(Channel ch) throws CertificateException {
		try {
			ChannelPipeline pipeline = ch.pipeline();
			SslHandler sslHandler = (SslHandler) pipeline.get(ReloadStack.HANDLER_SSL);
			Certificate remoteCert = sslHandler.engine().getSession().getPeerCertificates()[0];
			return cryptoHelper.getCertificateParser().parse(remoteCert);
		} catch (Exception e) {
			throw new CertificateException(e);
		}
	}

	void clientDisonnected(Channel channel) {
		Logger.getRootLogger().debug("Client disconnected");
		// TODO
	}

	public Connection getConnection(NodeID neighbor) {
		return connections.get(neighbor);
	}

	public OverlayLinkType getServerProtocol() {
		return SERVER_PROTO;
	}

	public InetSocketAddress getServerAddress() {
		return (InetSocketAddress) attachServer.getChannel().localAddress();
	}
}
