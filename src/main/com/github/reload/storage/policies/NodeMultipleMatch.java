package com.github.reload.storage.policies;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Set;
import javax.naming.ConfigurationException;
import com.github.reload.Configuration;
import com.github.reload.DataKind;
import com.github.reload.ReloadOverlay;
import com.github.reload.crypto.CryptoHelper;
import com.github.reload.crypto.ReloadCertificate;
import com.github.reload.message.CertHashNodeIdSignerIdentityValue;
import com.github.reload.message.HashAlgorithm;
import com.github.reload.message.NodeID;
import com.github.reload.message.ResourceID;
import com.github.reload.message.SignerIdentity;
import com.github.reload.message.SignerIdentity.IdentityType;
import com.github.reload.message.SignerIdentityValue;
import com.github.reload.storage.data.StoredData;

/**
 * Check if the nodeid hash in the sender certificate concatenated with an index
 * value matches the resource id
 * 
 */
public class NodeMultipleMatch extends AccessPolicy {

	private static final String NAME = "node-multiple";

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public void accept(ResourceID resourceId, StoredData data, SignerIdentity signerIdentity, Configuration conf) throws AccessPolicyException {
		if (signerIdentity.getIdentityType() != IdentityType.CERT_HASH_NODE_ID)
			throw new AccessPolicyException("Wrong signer identity type");
		long maxIndex = data.getKind().getLongAttribute(DataKind.ATTR_MAX_NODE_MULTIPLE);

		validate(resourceId, signerIdentity, maxIndex, context);
	}

	@Override
	public void checkKindParams(DataKind.Builder dataKindBuilder) throws ConfigurationException {
		if (dataKindBuilder.getLongAttribute(DataKind.ATTR_MAX_NODE_MULTIPLE) == 0)
			throw new ConfigurationException("The node-multiple access policy requires max-node-multiple parameter");
	}

	private static void validate(ResourceID resourceId, SignerIdentity storerIdentity, long maxIndex, Configuration conf) throws AccessPolicyException {

		ReloadCertificate storerReloadCert = context.getCryptoHelper().getCertificate(storerIdentity);
		if (storerReloadCert == null)
			throw new AccessPolicyException("Unknown signer identity");

		Set<NodeID> storerNodeIds = storerReloadCert.getNodeIds();

		X509Certificate storerCert = (X509Certificate) storerReloadCert.getOriginalCertificate();

		byte[] resourceIdHash = resourceId.getData();

		for (NodeID storerNodeId : storerNodeIds) {
			for (int curIndex = 0; curIndex <= maxIndex; curIndex++) {
				byte[] nodeIdHash = hashIndexedNodeId(CryptoHelper.OVERLAY_HASHALG, storerNodeId, curIndex, context);
				if (Arrays.equals(nodeIdHash, resourceIdHash)) {
					checkIdentityHash(storerCert, storerNodeId, storerIdentity);
					return;
				}
			}
		}

		throw new AccessPolicyException("Matching node-id not found in signer certificate");
	}

	private static byte[] hashIndexedNodeId(HashAlgorithm hashAlg, NodeID storerId, int index, Configuration conf) {
		int length = context.getTopologyPlugin().getResourceIdLength();
		try {
			MessageDigest d = MessageDigest.getInstance(hashAlg.toString());
			d.update(storerId.getData());
			return Arrays.copyOfRange(d.digest(ByteBuffer.allocate(4).putInt(index).array()), 0, length);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	private static void checkIdentityHash(Certificate storerCert, NodeID storerId, SignerIdentity storerIdentity) throws AccessPolicyException {
		SignerIdentityValue storerIdentityValue = storerIdentity.getSignerIdentityValue();

		byte[] computedIdentityValue = CertHashNodeIdSignerIdentityValue.computeHash(storerIdentityValue.getCertHashAlg(), storerCert, storerId);

		if (!Arrays.equals(storerIdentityValue.getHashValue(), computedIdentityValue))
			throw new AccessPolicyException("Identity hash value mismatch");
	}

	/**
	 * Parameters generator for NODE-MULTIPLE policy
	 * 
	 * @author Daniel Zozin <zdenial@gmx.com>
	 * 
	 */
	public static class NodeMultipleParamsGenerator extends AccessPolicyParamsGenerator {

		public NodeMultipleParamsGenerator(ReloadOverlay conn) {
			super(conn);
		}

		public ResourceID getResourceId(NodeID storerId, int nodeIndex) {
			HashAlgorithm overlayHashAlg = CryptoHelper.OVERLAY_HASHALG;
			return context.getTopologyPlugin().getResourceId(hashIndexedNodeId(overlayHashAlg, storerId, nodeIndex, context));
		}
	}

	@Override
	public AccessPolicyParamsGenerator getParamsGenerator(ReloadOverlay conn) {
		return new NodeMultipleParamsGenerator(conn);
	}
}
