package com.github.reload.storage.net;

import io.netty.buffer.ByteBuf;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import com.github.reload.Context;
import com.github.reload.message.NodeID;
import com.github.reload.net.data.Codec;
import com.github.reload.storage.DataKind;

public class StoreKindResponse {

	private final DataKind kind;
	private final BigInteger generationCounter;
	private final List<NodeID> replicas;

	public StoreKindResponse(DataKind kind, BigInteger generationCounter, List<NodeID> replicas) {
		this.kind = kind;
		this.generationCounter = generationCounter;
		this.replicas = replicas;
	}

	public BigInteger getGenerationCounter() {
		return generationCounter;
	}

	public DataKind getKind() {
		return kind;
	}

	public List<NodeID> getReplicas() {
		return replicas;
	}

	@Override
	public String toString() {
		return "StoreKindResponse [kind=" + kind.getKindId() + ", generation=" + generationCounter + ", replicas=" + replicas + "]";
	}

	public static class StoreKindResponseCodec extends Codec<StoreKindResponse> {

		private static final int REPLICAS_LENGTH_FIELD = U_INT16;

		private final Codec<DataKind> kindCodec;
		private final Codec<NodeID> nodeIdCodec;

		public StoreKindResponseCodec(Context context) {
			super(context);
			kindCodec = getCodec(DataKind.class);
			nodeIdCodec = getCodec(NodeID.class);
		}

		@Override
		public void encode(StoreKindResponse obj, ByteBuf buf, Object... params) throws CodecException {
			kindCodec.encode(obj.kind, buf);
			buf.writeBytes(obj.generationCounter.toByteArray());

			Field lenFld = allocateField(buf, REPLICAS_LENGTH_FIELD);

			for (NodeID n : obj.replicas) {
				nodeIdCodec.encode(n, buf);
			}

			lenFld.updateDataLength();
		}

		@Override
		public StoreKindResponse decode(ByteBuf buf, Object... params) throws CodecException {
			DataKind kind = kindCodec.decode(buf);

			byte[] genCounterData = new byte[8];
			buf.readBytes(genCounterData);
			BigInteger genCounter = new BigInteger(1, genCounterData);

			List<NodeID> replicas = decodeReplicas(buf);
			return new StoreKindResponse(kind, genCounter, replicas);
		}

		private List<NodeID> decodeReplicas(ByteBuf buf) throws com.github.reload.net.data.Codec.CodecException {
			ByteBuf replicasFld = readField(buf, REPLICAS_LENGTH_FIELD);

			List<NodeID> replicas = new ArrayList<NodeID>();

			while (replicasFld.readableBytes() > 0) {
				replicas.add(nodeIdCodec.decode(replicasFld));
			}

			return replicas;
		}

	}
}
