package com.github.reload.storage.policies;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Set;
import net.sf.jReload.AccessPolicyParamsGenerator;
import net.sf.jReload.Context;
import net.sf.jReload.ReloadOverlay;
import net.sf.jReload.configuration.ConfigurationException;
import net.sf.jReload.crypto.CryptoHelper;
import net.sf.jReload.crypto.ReloadCertificate;
import net.sf.jReload.crypto.reload.CertHashNodeIdSignerIdentityValue;
import net.sf.jReload.crypto.reload.HashAlgorithm;
import net.sf.jReload.crypto.reload.SignerIdentity;
import net.sf.jReload.crypto.reload.SignerIdentity.IdentityType;
import net.sf.jReload.crypto.reload.SignerIdentityValue;
import net.sf.jReload.message.NodeID;
import net.sf.jReload.message.ResourceID;
import net.sf.jReload.storage.DataKind;
import net.sf.jReload.storage.StoredData;

/**
 * Check if the nodeid hash in the sender certificate concatenated with an index
 * value matches the resource id
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class NodeMultipleMatch extends AccessPolicy {

	protected NodeMultipleMatch() {
		super("node-multiple");
	}

	@Override
	public void accept(ResourceID resourceId, StoredData data, SignerIdentity signerIdentity, Context context) throws AccessPolicyException {
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

	private static void validate(ResourceID resourceId, SignerIdentity storerIdentity, long maxIndex, Context context) throws AccessPolicyException {

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

	private static byte[] hashIndexedNodeId(HashAlgorithm hashAlg, NodeID storerId, int index, Context context) {
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
