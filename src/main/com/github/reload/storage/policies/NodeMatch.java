package com.github.reload.storage.policies;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import com.github.reload.Components;
import com.github.reload.crypto.CryptoHelper;
import com.github.reload.crypto.ReloadCertificate;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.encoders.header.ResourceID;
import com.github.reload.net.encoders.secBlock.CertHashNodeIdSignerIdentityValue;
import com.github.reload.net.encoders.secBlock.HashAlgorithm;
import com.github.reload.net.encoders.secBlock.SignerIdentity;
import com.github.reload.net.encoders.secBlock.SignerIdentity.IdentityType;
import com.github.reload.net.encoders.secBlock.SignerIdentityValue;
import com.github.reload.routing.TopologyPlugin;
import com.github.reload.storage.AccessPolicy;
import com.github.reload.storage.AccessPolicy.PolicyName;
import com.github.reload.storage.encoders.StoredData;

/**
 * Check if the nodeid hash in the sender certificate matches the resource id
 * 
 */
@PolicyName("node-match")
public class NodeMatch extends AccessPolicy {

	@Override
	public void accept(ResourceID resourceId, StoredData data, SignerIdentity signerIdentity) throws AccessPolicyException {
		if (signerIdentity.getIdentityType() != IdentityType.CERT_HASH_NODE_ID)
			throw new AccessPolicyException("Wrong signer identity type");

		validate(resourceId, signerIdentity);
	}

	private static void validate(ResourceID resourceId, SignerIdentity storerIdentity) throws AccessPolicyException {
		CryptoHelper<?> crypto = (CryptoHelper<?>) Components.get(CryptoHelper.COMPNAME);
		ReloadCertificate storerReloadCert = crypto.getCertificate(storerIdentity);
		if (storerReloadCert == null)
			throw new AccessPolicyException("Unknown signer identity");

		NodeID storerNodeId = storerReloadCert.getNodeId();

		byte[] resourceIdHash = resourceId.getData();

		X509Certificate storerCert = (X509Certificate) storerReloadCert.getOriginalCertificate();

		byte[] nodeIdHash = hashNodeId(CryptoHelper.OVERLAY_HASHALG, storerNodeId);
		if (Arrays.equals(nodeIdHash, resourceIdHash)) {
			checkIdentityHash(storerCert, storerNodeId, storerIdentity);
			return;
		}

		throw new AccessPolicyException("Matching node-id not found in signer certificate");
	}

	private static byte[] hashNodeId(HashAlgorithm hashAlg, NodeID storerId) {
		TopologyPlugin plugin = (TopologyPlugin) Components.get(TopologyPlugin.COMPNAME);
		int length = plugin.getResourceIdLength();
		try {
			MessageDigest d = MessageDigest.getInstance(hashAlg.toString());
			return Arrays.copyOfRange(d.digest(storerId.getData()), 0, length);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	private static void checkIdentityHash(Certificate storerCert, NodeID storerId, SignerIdentity storerIdentity) throws AccessPolicyException {
		SignerIdentityValue storerIdentityValue = storerIdentity.getSignerIdentityValue();

		byte[] computedIdentityValue = CertHashNodeIdSignerIdentityValue.computeHash(storerIdentityValue.getHashAlgorithm(), storerCert, storerId);

		if (!Arrays.equals(storerIdentityValue.getHashValue(), computedIdentityValue))
			throw new AccessPolicyException("Identity hash value mismatch");
	}

	/**
	 * Parameters generator for NODE-MATCH policy
	 * 
	 */
	public static class NodeParamsGenerator implements AccessPolicyParamsGenerator {

		public ResourceID getResourceId(NodeID storerId) {
			TopologyPlugin plugin = (TopologyPlugin) Components.get(TopologyPlugin.COMPNAME);
			return plugin.getResourceId(hashNodeId(CryptoHelper.OVERLAY_HASHALG, storerId));
		}
	}

	@Override
	public AccessPolicyParamsGenerator getParamsGenerator() {
		return new NodeParamsGenerator();
	}
}
