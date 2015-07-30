package com.github.reload;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.log4j.Logger;
import com.github.reload.components.ComponentsContext;
import com.github.reload.components.ComponentsContext.CompStop;
import com.github.reload.components.MessageHandlersManager.MessageHandler;
import com.github.reload.conf.Configuration;
import com.github.reload.crypto.CryptoHelper;
import com.github.reload.crypto.Keystore;
import com.github.reload.crypto.MemoryKeystore;
import com.github.reload.crypto.ReloadCertificate;
import com.github.reload.crypto.X509CertificateParser;
import com.github.reload.crypto.X509CryptoHelper;
import com.github.reload.net.MessageRouter;
import com.github.reload.net.connections.Connection;
import com.github.reload.net.connections.ConnectionManager;
import com.github.reload.net.connections.ConnectionManager.ConnectionStatusEvent;
import com.github.reload.net.connections.ConnectionManager.ConnectionStatusEvent.Type;
import com.github.reload.net.encoders.Header;
import com.github.reload.net.encoders.Message;
import com.github.reload.net.encoders.MessageBuilder;
import com.github.reload.net.encoders.content.ContentType;
import com.github.reload.net.encoders.content.Error.ErrorType;
import com.github.reload.net.encoders.content.JoinAnswer;
import com.github.reload.net.encoders.content.JoinRequest;
import com.github.reload.net.encoders.content.LeaveRequest;
import com.github.reload.net.encoders.header.DestinationList;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.encoders.header.ResourceID;
import com.github.reload.net.encoders.header.RoutableID;
import com.github.reload.net.encoders.secBlock.SignatureAlgorithm;
import com.github.reload.net.ice.HostCandidate.OverlayLinkType;
import com.github.reload.routing.RoutingTable;
import com.github.reload.routing.TopologyPlugin;
import com.github.reload.services.storage.local.StoredKindData;
import com.github.reload.services.storage.net.StoreRequest;
import com.google.common.base.Optional;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import dagger.Module;
import dagger.Provides;

public class TestFactory extends BootstrapFactory {

	@Override
	public boolean isCompatibleWith(Configuration conf) {
		return true;
	}

	@Override
	protected TestBootstrap createBootstrap(Configuration conf) {
		return new TestBootstrap(conf);
	}

	@Module(injects = {TestConfiguration.class, Overlay.class,
						TestPlugin.class, X509CryptoHelper.class,
						MemoryKeystore.class}, library = true, complete = false)
	public static class TestModule {

		private final Keystore keystore;
		private final CryptoHelper cryptoHelper;

		public TestModule(Bootstrap boot) {
			keystore = new MemoryKeystore(boot.getLocalCert(), boot.getLocalKey());
			cryptoHelper = new X509CryptoHelper(keystore, boot.getConfiguration(), boot.getSignHashAlg(), boot.getSignAlg(), boot.getHashAlg());
		}

		@Provides
		@Singleton
		Configuration provideConfiguration() {
			return new TestConfiguration();
		}

		@Provides
		@Singleton
		TopologyPlugin provideTopologyPlugin() {
			return new TestPlugin();
		}

		@Provides
		@Singleton
		public Keystore provideKeystore() {
			return keystore;
		}

		@Provides
		@Singleton
		public CryptoHelper provideCryptoHelper(Keystore keystore, Configuration conf) {
			return cryptoHelper;
		}
	}

	public static class TestBootstrap extends Bootstrap {

		public TestBootstrap(Configuration conf) {
			super(conf);
		}

		@Override
		protected List<Object> getProviderModules() {
			return Collections.singletonList(new TestModule(this));
		}

		@Override
		protected byte[] getJoinData() {
			return "JOIN REQ".getBytes();
		}

		public static ReloadCertificate loadCert(String certPath) throws CertificateException, FileNotFoundException {
			return X509CertificateParser.parse((X509Certificate) loadLocalCert(certPath));
		}

		public static Certificate loadLocalCert(String localCertPath) throws FileNotFoundException, CertificateException {
			if (localCertPath == null || !new File(localCertPath).exists())
				throw new FileNotFoundException("Overlay certificate file not found at " + localCertPath);

			CertificateFactory certFactory;
			try {
				certFactory = CertificateFactory.getInstance("X.509");
				File overlayCertFile = new File(localCertPath);
				InputStream certStream = new FileInputStream(overlayCertFile);
				Certificate cert = certFactory.generateCertificate(certStream);
				certStream.close();
				return cert;
			} catch (CertificateException | IOException e) {
				throw new CertificateException(e);
			}
		}

		public static PrivateKey loadPrivateKey(String privateKeyPath, SignatureAlgorithm keyAlg) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
			if (privateKeyPath == null || !new File(privateKeyPath).exists())
				throw new FileNotFoundException("Private key file not found at " + privateKeyPath);

			File file = new File(privateKeyPath);
			byte[] privKeyBytes = new byte[(int) file.length()];
			InputStream in = new FileInputStream(file);
			in.read(privKeyBytes);
			in.close();
			KeyFactory keyFactory = KeyFactory.getInstance(keyAlg.toString());
			KeySpec ks = new PKCS8EncodedKeySpec(privKeyBytes);
			return keyFactory.generatePrivate(ks);
		}
	}

	public static class TestPlugin implements TopologyPlugin {

		private static final int RESID_LENGTH = 16;

		private static final Logger l = Logger.getRootLogger();

		ComponentsContext ctx;

		@Inject
		Overlay overlay;

		@Inject
		Configuration conf;

		@Inject
		ConnectionManager connMgr;

		@Inject
		MessageRouter router;

		@Inject
		MessageBuilder msgBuilder;

		private boolean isJoined = false;

		private final TestRouting r = new TestRouting();

		private final NodeID TEST_REPLICA_NODE = NodeID.valueOf(new byte[16]);

		@Override
		public void startAgent() {
			if (overlay.isOverlayInitiator()) {
				try {
					addLoopback().get();
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
			}
		}

		@Override
		public ListenableFuture<NodeID> requestJoin() {
			l.info(String.format("Joining to RELOAD overlay %s with %s in progress...", conf.get(Configuration.OVERLAY_NAME), overlay.getLocalNodeId()));

			JoinRequest req = new JoinRequest(overlay.getLocalNodeId(), overlay.getJoinData());

			DestinationList dest = new DestinationList(ResourceID.valueOf(overlay.getLocalNodeId().getData()));

			Message request = msgBuilder.newMessage(req, dest);

			ListenableFuture<Message> joinAnsFut = router.sendRequestMessage(request);

			final SettableFuture<NodeID> joinFuture = SettableFuture.create();

			Futures.addCallback(joinAnsFut, new FutureCallback<Message>() {

				@Override
				public void onSuccess(Message joinAns) {
					NodeID ap = joinAns.getHeader().getSenderId();
					addLoopback();
					r.neighbors.add(ap);
					l.info(String.format("Joining to RELOAD overlay %s with %s completed.", conf.get(Configuration.OVERLAY_NAME), overlay.getLocalNodeId()));
					isJoined = true;
					joinFuture.set(joinAns.getHeader().getSenderId());
				};

				@Override
				public void onFailure(Throwable t) {
					l.info(String.format("Joining to RELOAD overlay %s with %s failed: %s", conf.get(Configuration.OVERLAY_NAME), overlay.getLocalNodeId(), t.getMessage()));
					joinFuture.setException(t);
				}
			});

			return joinFuture;
		}

		private ListenableFuture<Connection> addLoopback() {
			ListenableFuture<Connection> fut = connMgr.connectTo(overlay.getLocalAddress(), OverlayLinkType.TLS_TCP_FH_NO_ICE);
			Futures.addCallback(fut, new FutureCallback<Connection>() {

				@Override
				public void onSuccess(Connection result) {
					r.neighbors.add(result.getNodeId());
				}

				@Override
				public void onFailure(Throwable t) {
					t.printStackTrace();
				}
			});
			return fut;
		}

		@MessageHandler(ContentType.JOIN_REQ)
		public void handleJoinRequest(final Message req) {

			JoinAnswer ans = new JoinAnswer("JOIN ANS".getBytes());

			router.sendAnswer(req.getHeader(), ans);

			NodeID joinNode = ((JoinRequest) req.getContent()).getJoiningNode();

			if (connMgr.isNeighbor(joinNode)) {
				r.neighbors.add(joinNode);
			}
		}

		@MessageHandler(ContentType.LEAVE_REQ)
		public void handleLeaveRequest(Message req) {
			Header head = req.getHeader();
			LeaveRequest leave = (LeaveRequest) req.getContent();
			NodeID leavingNode = leave.getLeavingNode();

			// Check sender id matches with the leaving node
			if (!head.getSenderId().equals(leavingNode)) {
				router.sendError(head, ErrorType.FORBITTEN, "Leaving node doesn't match with sender ID");
				return;
			}

			NodeID prevHop = head.getAttribute(Header.PREV_HOP);

			// Check neighbor id matches with the leaving node
			if (!prevHop.equals(leavingNode)) {
				router.sendError(head, ErrorType.FORBITTEN, "Leaving node is not a neighbor node");
				return;
			}

			r.neighbors.remove(leavingNode);

			l.debug(String.format("Node %s has left the overlay", leavingNode));
		}

		@Subscribe
		public void handleConnectionEvent(ConnectionStatusEvent e) {
			if (e.type == Type.ESTABLISHED) {
				r.neighbors.add(e.connection.getNodeId());
			}

			if (e.type == Type.CLOSED) {
				r.neighbors.remove(e.connection.getNodeId());
			}
		}

		@Override
		public int getResourceIdLength() {
			return RESID_LENGTH;
		}

		@Override
		public int getDistance(RoutableID source, RoutableID dest) {
			BigInteger f = new BigInteger(source.getData());
			BigInteger s = new BigInteger(dest.getData());
			return f.subtract(s).abs().mod(BigInteger.valueOf(Integer.MAX_VALUE)).intValue();
		}

		@Override
		public ResourceID getResourceId(byte[] resourceIdentifier) {
			return ResourceID.valueOf(Arrays.copyOfRange(resourceIdentifier, 0, RESID_LENGTH));
		}

		@Override
		public <T extends RoutableID> T getCloserId(RoutableID destination, Collection<T> ids) {
			if (destination == null)
				throw new NullPointerException();
			int dist = Integer.MAX_VALUE;
			T closer = null;
			for (T id : ids) {
				int tmp = getDistance(id, destination);
				if (tmp <= dist) {
					dist = tmp;
					closer = id;
				}
			}
			return closer;
		}

		@Override
		public boolean isLocalPeerResponsible(RoutableID destination) {
			int localDistance = Integer.MAX_VALUE;

			if (isJoined || overlay.isOverlayInitiator()) {
				localDistance = getDistance(destination, overlay.getLocalNodeId());
			}

			for (NodeID neighborId : r.neighbors) {
				int tmpDst = getDistance(neighborId, destination);
				if (tmpDst < localDistance)
					return false;
			}

			return true;
		}

		@Override
		public ListenableFuture<Void> requestLeave() {
			SettableFuture<Void> fut = SettableFuture.create();
			for (NodeID n : r.neighbors) {
				sendLeave(n);
			}

			fut.set(null);

			return fut;
		}

		private void sendLeave(final NodeID neighborNode) {
			DestinationList dest = new DestinationList();
			dest.add(neighborNode);
			dest.add(msgBuilder.getWildcard());

			Message leaveMessage = msgBuilder.newMessage(new LeaveRequest(overlay.getLocalNodeId(), new byte[0]), dest);

			router.sendMessage(leaveMessage);
		}

		@CompStop
		private void stop() {
			sendLeaveAndClose();
		}

		private void sendLeaveAndClose() {

		}

		private class TestRouting implements RoutingTable {

			private final SortedSet<NodeID> neighbors = new TreeSet<NodeID>();

			@Override
			public Set<NodeID> getNextHops(RoutableID destination) {

				Set<NodeID> candidates = new HashSet<NodeID>(neighbors);

				if (candidates.isEmpty())
					return Collections.emptySet();

				// Remove loopback connection from results if destination is not
				// itself and there are other available neighbors
				if (!destination.equals(overlay.getLocalNodeId()) && candidates.size() > 1) {
					candidates.remove(overlay.getLocalNodeId());
				}

				NodeID singleNextHop = getCloserId(destination, candidates);

				return Collections.singleton(singleNextHop);
			}

			@Override
			public Set<NodeID> getNeighbors() {
				return Collections.unmodifiableSet(neighbors);
			}
		}

		@Override
		public ListenableFuture<NodeID> requestUpdate(NodeID neighborNode) {
			// NO update
			return null;
		}

		@Override
		public boolean isLocalPeerValidStorage(ResourceID resourceId, boolean isReplica) {
			return true;
		}

		@Override
		public List<NodeID> getReplicaNodes(ResourceID resourceId) {
			return Collections.singletonList(TEST_REPLICA_NODE);
		}

		@Override
		public void requestReplication(ResourceID resourceId) {
			List<NodeID> replicaNodes = getReplicaNodes(resourceId);

			Optional<Map<Long, StoredKindData>> res = store.get(resourceId);

			if (!res.isPresent())
				return;

			Collection<StoredKindData> data = res.get().values();

			short replNum = 1;
			for (NodeID repl : replicaNodes) {
				replicateData(msgBuilder.newMessage(new StoreRequest(resourceId, replNum, data), new DestinationList(repl)));
				replNum++;
			}
		}

		private void replicateData(Message replicaStore) {
			router.sendRequestMessage(replicaStore);
		}

		@Override
		public RoutingTable getRoutingTable() {
			return r;
		}
	}
}