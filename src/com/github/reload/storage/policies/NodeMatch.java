package com.github.reload.storage.policies;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Set;
import com.github.reload.message.CertHashNodeIdSignerIdentityValue;
import com.github.reload.message.HashAlgorithm;
import com.github.reload.message.SignerIdentity;
import com.github.reload.message.SignerIdentityValue;
import com.github.reload.message.SignerIdentity.IdentityType;
import net.sf.jReload.AccessPolicyParamsGenerator;
import net.sf.jReload.Context;
import net.sf.jReload.ReloadOverlay;
import net.sf.jReload.crypto.CryptoHelper;
import net.sf.jReload.crypto.ReloadCertificate;
import net.sf.jReload.message.NodeID;
import net.sf.jReload.message.ResourceID;
import net.sf.jReload.storage.StoredData;

/**
 * Check if the nodeid hash in the sender certificate matches the resource id
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class NodeMatch extends AccessPolicy {

	protected NodeMatch() {
		super("node-match");
	}

	@Override
	public void accept(ResourceID resourceId, StoredData data, SignerIdentity signerIdentity, Context context) throws AccessPolicyException {
		if (signerIdentity.getIdentityType() != IdentityType.CERT_HASH_NODE_ID)
			throw new AccessPolicyException("Wrong signer identity type");

		validate(resourceId, signerIdentity, context);
	}

	private static void validate(ResourceID resourceId, SignerIdentity storerIdentity, Context context) throws AccessPolicyException {

		ReloadCertificate storerReloadCert = context.getCryptoHelper().getCertificate(storerIdentity);
		if (storerReloadCert == null)
			throw new AccessPolicyException("Unknown signer identity");

		Set<NodeID> storerNodeIds = storerReloadCert.getNodeIds();

		byte[] resourceIdHash = resourceId.getData();

		X509Certificate storerCert = (X509Certificate) storerReloadCert.getOriginalCertificate();

		for (NodeID storerNodeId : storerNodeIds) {
			byte[] nodeIdHash = hashNodeId(CryptoHelper.OVERLAY_HASHALG, storerNodeId, context);
			if (Arrays.equals(nodeIdHash, resourceIdHash)) {
				checkIdentityHash(storerCert, storerNodeId, storerIdentity);
				return;
			}
		}

		throw new AccessPolicyException("Matching node-id not found in signer certificate");
	}

	private static byte[] hashNodeId(HashAlgorithm hashAlg, NodeID storerId, Context context) {
		int length = context.getTopologyPlugin().getResourceIdLength();
		try {
			MessageDigest d = MessageDigest.getInstance(hashAlg.toString());
			return Arrays.copyOfRange(d.digest(storerId.getData()), 0, length);
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
	 * Parameters generator for NODE-MATCH policy
	 * 
	 * @author Daniel Zozin <zdenial@gmx.com>
	 * 
	 */
	public static class NodeParamsGenerator extends AccessPolicyParamsGenerator {

		public NodeParamsGenerator(ReloadOverlay conn) {
			super(conn);
		}

		public ResourceID getResourceId(NodeID storerId) {
			HashAlgorithm overlayHashAlg = CryptoHelper.OVERLAY_HASHALG;
			return context.getTopologyPlugin().getResourceId(hashNodeId(overlayHashAlg, storerId, context));
		}
	}

	@Override
	public AccessPolicyParamsGenerator getParamsGenerator(ReloadOverlay overlay) {
		return new NodeParamsGenerator(overlay);
	}
}
