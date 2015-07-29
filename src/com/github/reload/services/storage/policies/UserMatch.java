package com.github.reload.services.storage.policies;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import javax.inject.Inject;
import com.github.reload.crypto.CryptoHelper;
import com.github.reload.crypto.Keystore;
import com.github.reload.crypto.ReloadCertificate;
import com.github.reload.net.encoders.header.ResourceID;
import com.github.reload.net.encoders.secBlock.HashAlgorithm;
import com.github.reload.net.encoders.secBlock.SignerIdentity;
import com.github.reload.routing.TopologyPlugin;
import com.github.reload.services.storage.local.StoredData;
import com.github.reload.services.storage.policies.AccessPolicy.ResourceIDGenerator;
import com.github.reload.services.storage.policies.AccessPolicy.PolicyName;
import com.github.reload.services.storage.DataKind;
import com.google.common.base.Optional;

/**
 * Check if the username hash in the sender certificate matches the resource id
 * 
 */
@PolicyName(value = "user-match", paramGen = ResourceIDGenerator.class)
public class UserMatch extends AccessPolicy {

	@Inject
	TopologyPlugin topology;

	@Inject
	Keystore keystore;

	@Override
	public void accept(ResourceID resourceId, DataKind kind, StoredData data, SignerIdentity signerIdentity) throws AccessPolicyException {

		Optional<ReloadCertificate> storerReloadCert = keystore.getCertificate(signerIdentity);

		if (!storerReloadCert.isPresent())
			throw new AccessPolicyException("Unknown signer identity");

		String storerUsername = storerReloadCert.get().getUsername();

		byte[] resourceIdHash = resourceId.getData();

		byte[] nodeIdHash = hashUsername(CryptoHelper.OVERLAY_HASHALG, storerUsername, topology.getResourceIdLength());

		if (!Arrays.equals(nodeIdHash, resourceIdHash))
			throw new AccessPolicyException("Identity hash value mismatch");

	}

	private static byte[] hashUsername(HashAlgorithm hashAlg, String username, int resIdLength) {
		try {
			MessageDigest d = MessageDigest.getInstance(hashAlg.toString());
			return Arrays.copyOfRange(d.digest(username.getBytes()), 0, resIdLength);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Parameters generator for USER-MATCH policy
	 * 
	 */
	public static class UserRIDGenerator implements ResourceIDGenerator {

		@Inject
		TopologyPlugin topology;

		@Inject
		Keystore keystore;

		public ResourceID generateID() {
			String username = keystore.getLocalCert().getUsername();
			return topology.getResourceId(hashUsername(CryptoHelper.OVERLAY_HASHALG, username, topology.getResourceIdLength()));
		}
	}
}
