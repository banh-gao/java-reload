package com.github.reload.net.connections;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.ssl.SslHandler;
import java.net.InetSocketAddress;
import java.security.cert.CertStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Map;
import javax.net.ssl.SSLEngine;
import org.apache.log4j.Logger;
import com.github.reload.Components.Component;
import com.github.reload.Components.start;
import com.github.reload.Components.stop;
import com.github.reload.ReloadConnector;
import com.github.reload.conf.Configuration;
import com.github.reload.crypto.CryptoHelper;
import com.github.reload.crypto.ReloadCertificate;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.ice.IceCandidate.OverlayLinkType;
import com.github.reload.net.stack.MessageDispatcher;
import com.github.reload.net.stack.ReloadStack;
import com.github.reload.net.stack.ReloadStackBuilder;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

/**
 * Establish and manage connections for all neighbor nodes
 */
public class ConnectionManager {

	private static final Logger l = Logger.getRootLogger();
	private static final OverlayLinkType SERVER_PROTO = OverlayLinkType.TLS_TCP_FH_NO_ICE;

	@Component
	private CryptoHelper cryptoHelper;

	@Component
	private Configuration conf;

	@Component
	private ReloadConnector connector;

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
		ReloadStackBuilder b = new ReloadStackBuilder(conf, msgDispatcher);
		b.setLocalAddress(connector.getLocalAddr());
		b.setLinkType(SERVER_PROTO);
		SSLEngine sslEngine = cryptoHelper.getSSLEngine(SERVER_PROTO);
		if (sslEngine != null)
			b.setSslEngine(sslEngine);
		attachServer = b.buildStack();

		ChannelPipeline serverPipeline = attachServer.getChannel().pipeline();
		serverPipeline.addLast(serverStatusHandler);
	}

	@stop
	private void shutdown() {
		attachServer.shutdown();
		for (Connection c : connections.values())
			c.close();
	}

	public ListenableFuture<Connection> connectTo(final NodeID remoteId, final InetSocketAddress remoteAddr, OverlayLinkType linkType) {
		final ReloadStack stack;

		final SettableFuture<Connection> outcome = SettableFuture.create();

		try {
			ReloadStackBuilder b = new ReloadStackBuilder(conf, msgDispatcher);
			b.setLinkType(linkType);
			SSLEngine sslEngine = cryptoHelper.getSSLEngine(linkType);
			if (sslEngine != null)
				b.setSslEngine(sslEngine);
			stack = b.buildStack();
		} catch (Exception e) {
			outcome.setException(e);
			return outcome;
		}

		ChannelFuture cf = stack.connect(remoteAddr);

		cf.addListener(new ChannelFutureListener() {

			@Override
			public void operationComplete(ChannelFuture future) throws Exception {

				if (future.isSuccess()) {
					Connection c;
					try {
						c = addConnection(stack);
					} catch (CertificateException e) {
						future.channel().close();
						l.debug("Connection to remote peer " + remoteId + " at " + remoteAddr + " closed: Invalid RELOAD certificate");
						outcome.setException(e);
						return;
					}

					l.debug("Connection to " + remoteId + " at " + remoteAddr + " completed");
					outcome.set(c);

				} else {
					l.warn("Connection to " + remoteId + " at " + remoteAddr + " failed", future.cause());
					outcome.setException(future.cause());
				}

			}
		});

		return outcome;
	}

	void clientConnected(Channel channel) throws CertificateException, CertStoreException {
		addConnection(new ReloadStack(channel));
	}

	private Connection addConnection(ReloadStack stack) throws CertificateException, CertStoreException {
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
