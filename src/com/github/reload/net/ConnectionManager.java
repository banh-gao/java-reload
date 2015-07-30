package com.github.reload.net;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import java.net.InetSocketAddress;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import org.apache.log4j.Logger;
import dagger.ObjectGraph;
import com.github.reload.crypto.CryptoHelper;
import com.github.reload.crypto.Keystore;
import com.github.reload.crypto.ReloadCertificate;
import com.github.reload.net.ConnectionManager.ConnectionStatusEvent.Type;
import com.github.reload.net.codecs.Codec;
import com.github.reload.net.codecs.ForwardMessage;
import com.github.reload.net.codecs.Header;
import com.github.reload.net.codecs.Message;
import com.github.reload.net.codecs.MessageEncoder;
import com.github.reload.net.codecs.header.NodeID;
import com.github.reload.net.ice.HostCandidate.OverlayLinkType;
import com.github.reload.net.stack.MessageDispatcher;
import com.github.reload.net.stack.ReloadStack;
import com.github.reload.net.stack.ReloadStackBuilder.ClientStackBuilder;
import com.github.reload.net.stack.ReloadStackBuilder.ServerStackBuilder;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

/**
 * Establish and manage connections for all neighbor nodes
 */
@Singleton
public class ConnectionManager {

	private static final Logger l = Logger.getRootLogger();
	private static final OverlayLinkType SERVER_PROTO = OverlayLinkType.TLS_TCP_FH_NO_ICE;

	@Inject
	CryptoHelper cryptoHelper;

	@Inject
	Keystore keystore;

	ObjectGraph ctx;

	@Inject
	EventBus eventBus;

	@Inject
	MessageDispatcher msgDispatcher;

	@Inject
	@Named("packetsLooper")
	Executor packetsLooper;

	@Inject
	Provider<ClientStackBuilder> clientBuilderProv;

	@Inject
	Provider<ServerStackBuilder> serverBuilderProv;

	private final Map<NodeID, Connection> connections = Maps.newHashMap();

	private ReloadStack attachServer;

	public void startServer(InetSocketAddress localAddress) {
		ServerStackBuilder b = serverBuilderProv.get();
		b.setLocalAddress(localAddress);
		b.setLinkType(SERVER_PROTO);

		attachServer = b.buildStack();
		l.debug(String.format("Server started at %s", attachServer.getChannel().localAddress()));
	}

	private void shutdown() {

		// FIXME: shotdown
		attachServer.shutdown();
	}

	public ListenableFuture<Connection> connectTo(final InetSocketAddress remoteAddr, OverlayLinkType linkType) {
		final ReloadStack stack;

		final SettableFuture<Connection> outcome = SettableFuture.create();

		try {
			ClientStackBuilder b = clientBuilderProv.get();
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

						@Override
						public void operationComplete(Future<Channel> future) throws Exception {

							packetsLooper.execute(new Runnable() {

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
									eventBus.post(new ConnectionStatusEvent(Type.ESTABLISHED, c));
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

	public void remoteNodeAccepted(final Channel channel) {
		SslHandler sslHandler = (SslHandler) channel.pipeline().get(ReloadStack.HANDLER_SSL);
		Future<Channel> handshakeFuture = sslHandler.handshakeFuture();

		handshakeFuture.addListener(new FutureListener<Channel>() {

			@Override
			public void operationComplete(final Future<Channel> future) throws Exception {
				packetsLooper.execute(new Runnable() {

					@Override
					public void run() {
						if (!future.isSuccess()) {
							future.cause().printStackTrace();
						}
						try {
							Connection c = addConnection(new ReloadStack(channel));
							eventBus.post(new ConnectionManager.ConnectionStatusEvent(Type.ACCEPTED, c));
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
		keystore.addCertificate(cert);
		Connection c = new Connection(cert.getNodeId(), stack);

		connections.put(cert.getNodeId(), c);
		return c;
	}

	private ReloadCertificate extractRemoteCert(Channel ch) throws CertificateException {
		try {
			ChannelPipeline pipeline = ch.pipeline();
			SslHandler sslHandler = (SslHandler) pipeline.get(ReloadStack.HANDLER_SSL);
			Certificate remoteCert = sslHandler.engine().getSession().getPeerCertificates()[0];
			return cryptoHelper.toReloadCertificate(remoteCert);
		} catch (Exception e) {
			throw new CertificateException(e);
		}
	}

	@Subscribe
	public void handlerConnectionEvent(ConnectionStatusEvent event) {
		switch (event.type) {
			case ACCEPTED :
				l.debug(String.format("Connection from %s at %s accepted", event.connection.getNodeId(), event.connection.getStack().getChannel().remoteAddress()));
				break;
			case CLOSED :
				l.debug(String.format("Connection with %s closed", event.connection.getNodeId()));
				break;
			case ESTABLISHED :
				l.debug("Connection to " + event.connection.getNodeId() + " at " + event.connection.getStack().getChannel().remoteAddress() + " completed");
				break;
		}
	}

	public Optional<Connection> getConnection(NodeID neighbor) {
		return Optional.fromNullable(connections.get(neighbor));
	}

	public boolean isNeighbor(NodeID nodeId) {
		return getConnection(nodeId).isPresent();
	}

	public OverlayLinkType getServerProtocol() {
		return SERVER_PROTO;
	}

	public InetSocketAddress getServerAddress() {
		return (InetSocketAddress) attachServer.getChannel().localAddress();
	}

	public static class ConnectionStatusEvent {

		public enum Type {
			ACCEPTED, ESTABLISHED, CLOSED
		}

		public final Type type;
		public final Connection connection;

		public ConnectionStatusEvent(Type type, Connection connection) {
			this.type = type;
			this.connection = connection;
		}

	}

	public Map<NodeID, Connection> getConnections() {
		return Collections.unmodifiableMap(connections);
	}

	/**
	 * A connection to a neighbor node
	 */
	public static class Connection {

		public static final AttributeKey<Connection> CONNECTION = AttributeKey.valueOf("reloadConnection");

		private final Codec<Header> hdrCodec;
		private final NodeID nodeId;
		private final ReloadStack stack;

		public Connection(NodeID nodeId, ReloadStack stack) {
			hdrCodec = Codec.getCodec(Header.class, null);
			this.nodeId = nodeId;
			this.stack = stack;
			stack.getChannel().attr(CONNECTION).set(this);
		}

		/**
		 * Send the given message to the neighbor
		 * 
		 * @param message
		 * @return
		 */
		public ChannelFuture write(Message message) {
			return stack.write(message);
		}

		/**
		 * Forward the given message to the neighbor
		 * 
		 * @param headedMessage
		 * @return
		 */
		public ChannelFuture forward(ForwardMessage headedMessage) {
			Channel ch = stack.getChannel();

			ByteBuf buf = ch.alloc().buffer();

			int messageStart = buf.writerIndex();

			try {
				hdrCodec.encode(headedMessage.getHeader(), buf);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

			buf.writeBytes(headedMessage.getPayload());

			setMessageLength(buf, messageStart);

			// Get the context of the layer just before link handler, the write
			// on
			// this context passes the given buffer to the link handler
			ChannelHandlerContext context = ch.pipeline().context(ReloadStack.DECODER_HEADER);

			return context.writeAndFlush(buf);
		}

		private void setMessageLength(ByteBuf buf, int messageStart) {
			int messageLength = buf.writerIndex() - messageStart;
			buf.setInt(messageStart + MessageEncoder.HDR_LEADING_LEN, messageLength);
		}

		public ChannelFuture close() {
			return stack.shutdown();
		}

		public NodeID getNodeId() {
			return nodeId;
		}

		/**
		 * @return The Maximum Transmission Unit this link can carry measured in
		 *         bytes. If an outgoing message exceedes this limit, the
		 *         message
		 *         will be automatically divided into smaller RELOAD fragments.
		 */
		protected int getLinkMTU() {
			return stack.getChannel().config().getOption(ChannelOption.SO_SNDBUF);
		}

		public ReloadStack getStack() {
			return stack;
		}

		@Override
		public String toString() {
			return "Connection to " + nodeId;
		}
	}
}
