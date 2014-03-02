package com.github.reload.storage.policies;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.naming.ConfigurationException;
import com.github.reload.Configuration;
import com.github.reload.DataKind;
import com.github.reload.message.ResourceID;
import com.github.reload.message.SignerIdentity;
import com.github.reload.storage.data.StoredData;
import com.github.reload.storage.errors.ForbittenException;

/**
 * An access control policy used by data kinds that determines if a store
 * request should be accepted
 * 
 */
public abstract class AccessPolicy {

	private static final Map<String, AccessPolicy> policies = new HashMap<String, AccessPolicy>();

	static {
		// Register default policies
		try {
			registerPolicy(NodeMatch.class);
			registerPolicy(NodeMultipleMatch.class);
			registerPolicy(UserMatch.class);
			registerPolicy(UserNodeMatch.class);
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	protected AccessPolicy() {
	}

	public static boolean registerPolicy(Class<? extends AccessPolicy> policyClazz) throws InstantiationException, IllegalAccessException {
		AccessPolicy policy = policyClazz.newInstance();
		policies.put(policy.getName().toLowerCase(), policy);
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

	public abstract String getName();

	/**
	 * Check if the store should be accepted
	 * 
	 * @throws ForbittenException
	 *             if the policy check fails
	 */
	public abstract void accept(ResourceID resourceId, StoredData data, SignerIdentity signerIdentity, Configuration conf) throws AccessPolicyException;

	/**
	 * Get a parameter generator for this access policy to be used with the
	 * specified overlay
	 */
	public abstract AccessPolicyParamsGenerator getParamsGenerator(Configuration conf);

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

	@Override
	public String toString() {
		return getName();
	}

	/**
	 * Indicates that the access control policy check fails
	 * 
	 */
	public static class AccessPolicyException extends ForbittenException {

		public AccessPolicyException(String message) {
			super("Access Policy check failed: " + message);
		}

	}

	/**
	 * Generate the parameters in conformity to an access control policy
	 * 
	 */
	public abstract class AccessPolicyParamsGenerator {

		protected final Configuration conf;

		public AccessPolicyParamsGenerator(Configuration conf) {
			this.conf = conf;
		}
	}
}
