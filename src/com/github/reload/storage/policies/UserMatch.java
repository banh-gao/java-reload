package com.github.reload.storage.policies;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import net.sf.jReload.AccessPolicyParamsGenerator;
import net.sf.jReload.Context;
import net.sf.jReload.ReloadOverlay;
import net.sf.jReload.crypto.CryptoHelper;
import net.sf.jReload.crypto.ReloadCertificate;
import net.sf.jReload.crypto.reload.HashAlgorithm;
import net.sf.jReload.crypto.reload.SignerIdentity;
import net.sf.jReload.message.ResourceID;
import net.sf.jReload.storage.StoredData;

/**
 * Check if the username hash in the sender certificate matches the resource id
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class UserMatch extends AccessPolicy {

	public UserMatch() {
		super("user-match");
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

	/**
	 * Parameters generator for USER-MATCH policy
	 * 
	 * @author Daniel Zozin <zdenial@gmx.com>
	 * 
	 */
	public static class UserParamsGenerator extends AccessPolicyParamsGenerator {

		public UserParamsGenerator(ReloadOverlay conn) {
			super(conn);
		}

		public ResourceID getResourceId(String username) {
			return context.getTopologyPlugin().getResourceId(hashUsername(CryptoHelper.OVERLAY_HASHALG, username, context));
		}
	}

	@Override
	public AccessPolicyParamsGenerator getParamsGenerator(ReloadOverlay conn) {
		return new UserParamsGenerator(conn);
	}
}
