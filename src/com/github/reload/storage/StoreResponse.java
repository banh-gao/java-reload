package com.github.reload.storage;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import com.github.reload.message.NodeID;

/**
 * A response contained in a store answer for a specific data kind, contains
 * informations about the store outcome
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class StoreResponse {

	private static final int REPLICAS_LENGTH_FIELD = EncUtils.U_INT16;

	private final DataKind kind;
	private final BigInteger generationCounter;
	private final List<NodeID> replicas;

	public StoreResponse(DataKind kind, BigInteger generationCounter, List<NodeID> replicas) {
		this.kind = kind;
		this.generationCounter = generationCounter;
		this.replicas = replicas;
	}

	public StoreResponse(Context context, UnsignedByteBuffer buf) throws UnknownKindException {
		KindId id = KindId.valueOf(buf);
		kind = context.getConfiguration().getDataKind(id);
		if (kind == null)
			throw new UnknownKindException(id);

		generationCounter = buf.getSigned64();
		replicas = getDecodedReplicas(context.getConfiguration().getNodeIdLength(), buf);
	}

	private static List<NodeID> getDecodedReplicas(int nodeIdLength, UnsignedByteBuffer buf) {
		int length = buf.getLengthValue(StoreResponse.REPLICAS_LENGTH_FIELD);

		List<NodeID> replicas = new ArrayList<NodeID>();

		int startPos = buf.position();

		while (buf.getConsumedFrom(startPos) < length) {
			NodeID id = NodeID.valueOf(nodeIdLength, buf);
			replicas.add(id);
		}
		return replicas;
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

	public void writeTo(UnsignedByteBuffer buf) {
		kind.getKindId().writeTo(buf);
		buf.putUnsigned64(generationCounter);

		Field lenFld = buf.allocateLengthField(REPLICAS_LENGTH_FIELD);

		for (NodeID n : replicas) {
			n.writeTo(buf);
		}

		lenFld.setEncodedLength(buf.getConsumedFrom(lenFld.getNextPosition()));
	}
}
