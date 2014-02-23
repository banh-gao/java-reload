package com.github.reload.storage.policies;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Set;
import com.github.reload.ReloadOverlay;
import com.github.reload.message.CertHashNodeIdSignerIdentityValue;
import com.github.reload.message.HashAlgorithm;
import com.github.reload.message.NodeID;
import com.github.reload.message.ResourceID;
import com.github.reload.message.SignerIdentity;
import com.github.reload.message.SignerIdentityValue;
import com.github.reload.storage.data.DictionaryValue;
import com.github.reload.storage.data.StoredData;

/**
 * Check if the username hash in the sender certificate matches the resource id
 * and nodeid hash matches dictionary key
 * 
 */
public class UserNodeMatch extends AccessPolicy {

	private static final String NAME = "user-node-match";

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public void accept(ResourceID resourceId, StoredData data, SignerIdentity signerIdentity, Context context) throws AccessPolicyException {
		ReloadCertificate storerReloadCert = context.getCryptoHelper().getCertificate(signerIdentity);
		if (storerReloadCert == null)
			throw new AccessPolicyException("Unknown signer identity");

		String storerUsername = storerReloadCert.getUsername();

		byte[] resourceIdHash = resourceId.getData();

		byte[] nodeIdHash = hashUsername(CryptoHelper.OVERLAY_HASHALG, storerUsername, context);

		if (!Arrays.equals(nodeIdHash, resourceIdHash))
			throw new AccessPolicyException("Identity hash value mismatch");

		validateKey(((DictionaryValue) data.getValue()).getKey(), storerReloadCert, signerIdentity, context);
	}

	private static void validateKey(Key key, ReloadCertificate storerReloadCert, SignerIdentity storerIdentity, Context context) throws AccessPolicyException {
		byte[] keyHash = key.getValue();

		Set<NodeID> storerNodeIds = storerReloadCert.getNodeIds();

		for (NodeID storerNodeId : storerNodeIds) {
			byte[] nodeIdHash = hashNodeId(CryptoHelper.OVERLAY_HASHALG, storerNodeId, context);
			if (Arrays.equals(nodeIdHash, keyHash)) {
				validateIdentityHash(storerReloadCert.getOriginalCertificate(), storerNodeId, storerIdentity);
				return;
			}
		}

		throw new AccessPolicyException("Wrong dictionary key");
	}

	private static byte[] hashUsername(HashAlgorithm hashAlg, String username, Context context) {
		int length = context.getTopologyPlugin().getResourceIdLength();
		try {
			MessageDigest d = MessageDigest.getInstance(hashAlg.toString());
			return Arrays.copyOfRange(d.digest(username.getBytes()), 0, length);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
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

	private static void validateIdentityHash(Certificate storerCert, NodeID storerId, SignerIdentity storerIdentity) throws AccessPolicyException {
		SignerIdentityValue storerIdentityValue = storerIdentity.getSignerIdentityValue();

		byte[] computedIdentityValue = CertHashNodeIdSignerIdentityValue.computeHash(storerIdentityValue.getCertHashAlg(), storerCert, storerId);

		if (!Arrays.equals(storerIdentityValue.getHashValue(), computedIdentityValue))
			throw new AccessPolicyException("Identity hash value mismatch");
	}

	/**
	 * Parameters generator for USER-NODE-MATCH policy
	 * 
	 * @author Daniel Zozin <zdenial@gmx.com>
	 * 
	 */
	public static class UserNodeParamsGenerator extends AccessPolicyParamsGenerator {

		public UserNodeParamsGenerator(ReloadOverlay conn) {
			super(conn);
		}

		public ResourceID getResourceId(String username) {
			return context.getTopologyPlugin().getResourceId(hashUsername(CryptoHelper.OVERLAY_HASHALG, username, context));
		}

		public Key getDictionaryKey(NodeID storerId) {
			HashAlgorithm overlayHashAlg = CryptoHelper.OVERLAY_HASHALG;
			return new Key(hashNodeId(overlayHashAlg, storerId, context));
		}
	}

	@Override
	public AccessPolicyParamsGenerator getParamsGenerator(ReloadOverlay conn) {
		return new UserNodeParamsGenerator(conn);
	}
}
