package com.github.reload.services.storage.policies;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.util.Arrays;
import com.github.reload.Overlay;
import com.github.reload.components.ComponentsContext;
import com.github.reload.crypto.CryptoHelper;
import com.github.reload.crypto.ReloadCertificate;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.encoders.header.ResourceID;
import com.github.reload.net.encoders.secBlock.CertHashNodeIdSignerIdentityValue;
import com.github.reload.net.encoders.secBlock.HashAlgorithm;
import com.github.reload.net.encoders.secBlock.SignerIdentity;
import com.github.reload.net.encoders.secBlock.SignerIdentityValue;
import com.github.reload.routing.TopologyPlugin;
import com.github.reload.services.storage.AccessPolicy;
import com.github.reload.services.storage.AccessPolicy.PolicyName;
import com.github.reload.services.storage.DataKind;
import com.github.reload.services.storage.encoders.DictionaryValue;
import com.github.reload.services.storage.encoders.StoredData;

/**
 * Check if the username hash in the sender certificate matches the resource id
 * and nodeid hash matches dictionary key
 * 
 */
@PolicyName("user-node-match")
public class UserNodeMatch extends AccessPolicy {

	@Override
	public void accept(ResourceID resourceId, DataKind kind, StoredData data, SignerIdentity signerIdentity, ComponentsContext ctx) throws AccessPolicyException {
		ReloadCertificate storerReloadCert = ctx.get(CryptoHelper.class).getCertificate(signerIdentity);
		if (storerReloadCert == null)
			throw new AccessPolicyException("Unknown signer identity");

		String storerUsername = storerReloadCert.getUsername();

		byte[] resourceIdHash = resourceId.getData();

		byte[] nodeIdHash = hashUsername(CryptoHelper.OVERLAY_HASHALG, storerUsername, ctx);

		if (!Arrays.equals(nodeIdHash, resourceIdHash))
			throw new AccessPolicyException("Identity hash value mismatch");

		validateKey(((DictionaryValue) data.getValue()).getKey(), storerReloadCert, signerIdentity, ctx);
	}

	private static void validateKey(byte[] key, ReloadCertificate storerReloadCert, SignerIdentity storerIdentity, ComponentsContext ctx) throws AccessPolicyException {
		NodeID storerNodeId = storerReloadCert.getNodeId();

		byte[] nodeIdHash = hashNodeId(CryptoHelper.OVERLAY_HASHALG, storerNodeId, ctx);

		if (Arrays.equals(nodeIdHash, key)) {
			validateIdentityHash(storerReloadCert.getOriginalCertificate(), storerNodeId, storerIdentity);
			return;
		}

		throw new AccessPolicyException("Wrong dictionary key");
	}

	private static byte[] hashUsername(HashAlgorithm hashAlg, String username, ComponentsContext ctx) {
		int length = ctx.get(TopologyPlugin.class).getResourceIdLength();
		try {
			MessageDigest d = MessageDigest.getInstance(hashAlg.toString());
			return Arrays.copyOfRange(d.digest(username.getBytes()), 0, length);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	private static byte[] hashNodeId(HashAlgorithm hashAlg, NodeID storerId, ComponentsContext ctx) {
		int length = ctx.get(TopologyPlugin.class).getResourceIdLength();
		try {
			MessageDigest d = MessageDigest.getInstance(hashAlg.toString());
			return Arrays.copyOfRange(d.digest(storerId.getData()), 0, length);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	private static void validateIdentityHash(Certificate storerCert, NodeID storerId, SignerIdentity storerIdentity) throws AccessPolicyException {
		SignerIdentityValue storerIdentityValue = storerIdentity.getSignerIdentityValue();

		byte[] computedIdentityValue = CertHashNodeIdSignerIdentityValue.computeHash(storerIdentityValue.getHashAlgorithm(), storerCert, storerId);

		if (!Arrays.equals(storerIdentityValue.getHashValue(), computedIdentityValue))
			throw new AccessPolicyException("Identity hash value mismatch");
	}

	@Override
	public AccessParamsGenerator getParamsGenerator() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Parameters generator for USER-NODE-MATCH policy
	 * 
	 * @author Daniel Zozin <zdenial@gmx.com>
	 * 
	 */
	public static class UserNodeParamsGenerator extends AccessParamsGenerator {

		public UserNodeParamsGenerator(Overlay conn) {
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
	public AccessParamsGenerator getParamsGenerator(Overlay conn) {
		return new UserNodeParamsGenerator(conn);
	}
}
