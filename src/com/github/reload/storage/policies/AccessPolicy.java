package com.github.reload.storage.policies;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import com.github.reload.message.SignerIdentity;
import net.sf.jReload.AccessPolicyParamsGenerator;
import net.sf.jReload.Context;
import net.sf.jReload.ReloadOverlay;
import net.sf.jReload.configuration.ConfigurationException;
import net.sf.jReload.message.ResourceID;
import net.sf.jReload.storage.DataKind;
import net.sf.jReload.storage.ForbittenException;
import net.sf.jReload.storage.StoredData;

/**
 * An access control policy used by data kinds that determines if a store
 * request should be accepted
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public abstract class AccessPolicy {

	private static final Map<String, AccessPolicy> policies = new HashMap<String, AccessPolicy>();

	static {
		// Register default policies
		registerPolicy(new NodeMatch());
		registerPolicy(new NodeMultipleMatch());
		registerPolicy(new UserMatch());
		registerPolicy(new UserNodeMatch());
	}

	private final String name;

	protected AccessPolicy(String name) {
		this.name = name;
	}

	public static boolean registerPolicy(AccessPolicy policy) {
		String name = policy.getName().toLowerCase();
		if (policies.containsKey(name))
			return false;

		policies.put(name, policy);
		return true;
	}

	public static boolean unregisterPolicy(String policyName) {
		String name = policyName.toLowerCase();
		return policies.remove(name) != null;
	}

	public static AccessPolicy getInstance(String policyName) {
		return policies.get(policyName.toLowerCase());
	}

	public static Map<String, AccessPolicy> getSupportedPolicies() {
		return Collections.unmodifiableMap(policies);
	}

	/**
	 * Check if the store should be accepted
	 * 
	 * @throws ForbittenException
	 *             if the policy check fails
	 */
	public abstract void accept(ResourceID resourceId, StoredData data, SignerIdentity signerIdentity, Context context) throws AccessPolicyException;

	/**
	 * Get a parameter generator for this access policy to be used with the
	 * specified overlay
	 */
	public abstract AccessPolicyParamsGenerator getParamsGenerator(ReloadOverlay conn);

	/**
	 * Throw a configuration exception if the given datakind builder doesn't
	 * contain the policy parameters required by this access control policy
	 * 
	 * @param dataKindBuilder
	 * @throws ConfigurationException
	 */
	public void checkKindParams(DataKind.Builder dataKindBuilder) throws ConfigurationException {
		// No parameter required by default
	}

	public final String getName() {
		return name;
	}

	@Override
	public String toString() {
		return getName();
	}

	/**
	 * Indicates that the access control policy check fails
	 * 
	 * @author Daniel Zozin <zdenial@gmx.com>
	 * 
	 */
	public static class AccessPolicyException extends ForbittenException {

		public AccessPolicyException(String message) {
			super("Policy check failed: " + message);
		}

	}
}
