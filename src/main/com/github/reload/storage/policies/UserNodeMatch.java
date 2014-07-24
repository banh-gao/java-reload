package com.github.reload.storage.policies;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Set;
import com.github.reload.ReloadOverlay;
import com.github.reload.crypto.CryptoHelper;
import com.github.reload.crypto.ReloadCertificate;
import com.github.reload.conf.Configuration;
import com.github.reload.net.encoders.content.storage.DictionaryValue;
import com.github.reload.net.encoders.content.storage.StoredData;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.encoders.header.ResourceID;
import com.github.reload.net.encoders.secBlock.CertHashNodeIdSignerIdentityValue;
import com.github.reload.net.encoders.secBlock.HashAlgorithm;
import com.github.reload.net.encoders.secBlock.SignerIdentity;
import com.github.reload.net.encoders.secBlock.SignerIdentityValue;
import com.github.reload.storage.AccessPolicy;
import com.github.reload.storage.AccessPolicy.PolicyName;

/**
 * Check if the username hash in the sender certificate matches the resource id
 * and nodeid hash matches dictionary key
 * 
 */
@PolicyName("user-node-match")
public class UserNodeMatch extends AccessPolicy {

	@Override
	public void accept(ResourceID resourceId, StoredData data, SignerIdentity signerIdentity, Configuration conf) throws AccessPolicyException {
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

	private static void validateKey(Key key, ReloadCertificate storerReloadCert, SignerIdentity storerIdentity, Configuration conf) throws AccessPolicyException {
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

	private static byte[] hashUsername(HashAlgorithm hashAlg, String username, Configuration conf) {
		int length = context.getTopologyPlugin().getResourceIdLength();
		try {
			MessageDigest d = MessageDigest.getInstance(hashAlg.toString());
			return Arrays.copyOfRange(d.digest(username.getBytes()), 0, length);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	private static byte[] hashNodeId(HashAlgorithm hashAlg, NodeID storerId, Configuration conf) {
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
