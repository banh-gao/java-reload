package com.github.reload.storage;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import com.github.reload.net.encoders.content.Error;
import com.github.reload.net.encoders.content.Error.ErrorMessageException;
import com.github.reload.net.encoders.content.Error.ErrorType;
import com.github.reload.net.encoders.header.ResourceID;
import com.github.reload.net.encoders.secBlock.SignerIdentity;
import com.github.reload.storage.encoders.StoredData;
import com.github.reload.storage.policies.NodeMatch;
import com.github.reload.storage.policies.NodeMultipleMatch;
import com.github.reload.storage.policies.UserMatch;
import com.github.reload.storage.policies.UserNodeMatch;

/**
 * An access control policy used by data kinds that determines if a store
 * request should be accepted
 * 
 */
public abstract class AccessPolicy {

	private static final Map<String, AccessPolicy> policies = new HashMap<String, AccessPolicy>();

	public static final Class<NodeMatch> NODE = NodeMatch.class;
	public static final Class<NodeMultipleMatch> NODE_MULTIPLE = NodeMultipleMatch.class;
	public static final Class<UserMatch> USER = UserMatch.class;
	public static final Class<UserNodeMatch> USER_NODE = UserNodeMatch.class;

	protected AccessPolicy() {
	}

	public static <T extends AccessPolicy> T getInstance(Class<T> clazz) {

		String name = getPolicyName(clazz);

		@SuppressWarnings("unchecked")
		T policy = (T) policies.get(name);

		if (policy == null) {
			try {
				policy = clazz.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				throw new RuntimeException(e);
			}
			policies.put(name, policy);
		}

		return policy;
	}

	private static String getPolicyName(Class<? extends AccessPolicy> clazz) {
		return clazz.getAnnotation(PolicyName.class).value().toLowerCase();
	}

	public static Map<String, AccessPolicy> getSupportedPolicies() {
		return Collections.unmodifiableMap(policies);
	}

	public String getName() {
		return getPolicyName(this.getClass());
	}

	/**
	 * Check if the store should be accepted
	 * 
	 * @throws ForbittenException
	 *             if the policy check fails
	 */
	public abstract void accept(ResourceID resourceId, StoredData data, SignerIdentity signerIdentity) throws AccessPolicyException;

	/**
	 * Get a parameter generator for this access policy to be used with the
	 * specified overlay
	 */
	public abstract AccessPolicyParamsGenerator getParamsGenerator();

	/**
	 * Throw an exception if the given datakind builder doesn't
	 * contain the policy parameters required by this access control policy
	 * 
	 * @param dataKindBuilder
	 */
	protected void checkKindParams(DataKind.Builder dataKindBuilder) {
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
	public static class AccessPolicyException extends ErrorMessageException {

		public AccessPolicyException(String message) {
			super(new Error(ErrorType.FORBITTEN, "Access Policy check failed: " + message));
		}

	}

	/**
	 * Generate the parameters in conformity to an access control policy
	 * 
	 */
	public interface AccessPolicyParamsGenerator {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface PolicyName {

		public String value();
	}
}
