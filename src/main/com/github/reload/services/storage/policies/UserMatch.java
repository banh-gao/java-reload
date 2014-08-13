package com.github.reload.services.storage.policies;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import com.github.reload.Bootstrap;
import com.github.reload.components.ComponentsContext;
import com.github.reload.crypto.CryptoHelper;
import com.github.reload.crypto.Keystore;
import com.github.reload.crypto.ReloadCertificate;
import com.github.reload.net.encoders.header.ResourceID;
import com.github.reload.net.encoders.secBlock.HashAlgorithm;
import com.github.reload.net.encoders.secBlock.SignerIdentity;
import com.github.reload.routing.TopologyPlugin;
import com.github.reload.services.storage.AccessPolicy;
import com.github.reload.services.storage.AccessPolicy.PolicyName;
import com.github.reload.services.storage.DataKind;
import com.github.reload.services.storage.encoders.StoredData;
import com.google.common.base.Optional;

/**
 * Check if the username hash in the sender certificate matches the resource id
 * 
 */
@PolicyName("user-match")
public class UserMatch extends AccessPolicy {

	@Override
	public void accept(ResourceID resourceId, DataKind kind, StoredData data, SignerIdentity signerIdentity, ComponentsContext ctx) throws AccessPolicyException {

		Optional<ReloadCertificate> storerReloadCert = ctx.get(Keystore.class).getCertificate(signerIdentity);

		if (!storerReloadCert.isPresent())
			throw new AccessPolicyException("Unknown signer identity");

		String storerUsername = storerReloadCert.get().getUsername();

		byte[] resourceIdHash = resourceId.getData();

		byte[] nodeIdHash = hashUsername(CryptoHelper.OVERLAY_HASHALG, storerUsername, ctx);

		if (!Arrays.equals(nodeIdHash, resourceIdHash))
			throw new AccessPolicyException("Identity hash value mismatch");

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

	@Override
	public AccessParamsGenerator newParamsGenerator(ComponentsContext ctx) {
		return new UserParamsGenerator(ctx);
	}

	/**
	 * Parameters generator for USER-MATCH policy
	 * 
	 */
	public static class UserParamsGenerator implements AccessParamsGenerator {

		private final ComponentsContext ctx;

		public UserParamsGenerator(ComponentsContext ctx) {
			this.ctx = ctx;
		}

		public ResourceID getResourceId() {
			String username = ctx.get(Bootstrap.class).getLocalCert().getUsername();
			return ctx.get(TopologyPlugin.class).getResourceId(hashUsername(CryptoHelper.OVERLAY_HASHALG, username, ctx));
		}
	}
}
