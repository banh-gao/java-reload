package com.github.reload;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import javax.naming.ConfigurationException;
import com.github.reload.net.encoders.secBlock.SecurityBlock;
import com.github.reload.storage.data.ArrayValue;
import com.github.reload.storage.data.DataModel;
import com.github.reload.storage.data.DataModel.DataValue;
import com.github.reload.storage.data.DataModel.ModelSpecifier;
import com.github.reload.storage.data.DictionaryValue;
import com.github.reload.storage.data.SingleValue;
import com.github.reload.storage.data.StoredDataSpecifier;
import com.github.reload.storage.errors.UnknownKindException;
import com.github.reload.storage.policies.AccessPolicy;
import com.github.reload.storage.policies.AccessPolicy.AccessPolicyParamsGenerator;

/**
 * The description of the data kind that a peer can handle
 * 
 */
public class DataKind {

	/**
	 * The data-kinds registered to IANA
	 * 
	 */
	public enum IANA {
		TURN_SERVICE("TURN-SERVICE", 2),
		CERTIFICATE_BY_NODE("CERTIFICATE_BY_NODE", 3),
		CERTIFICATE_BY_USER("CERTIFICATE_BY_USER", 16);

		private final String name;
		private final long kindId;

		private IANA(String name, long kindId) {
			this.name = name;
			this.kindId = kindId;
		}

		public long getKindId() {
			return kindId;
		}

		public String getName() {
			return name;
		}

		public static IANA getByName(String name) throws UnknownKindException {
			for (IANA k : EnumSet.allOf(IANA.class))
				if (name.equalsIgnoreCase(k.name))
					return k;
			throw new UnknownKindException("Unregistered IANA kind " + name);
		}

		public static String nameOf(long kindId) {
			for (IANA kind : EnumSet.allOf(IANA.class))
				if (kindId == kind.kindId)
					return kind.name;
			return "";
		}
	}

	private final long kindId;
	private final String name;
	private final DataModel<? extends DataValue> dataModel;
	private final AccessPolicy accessPolicy;
	private final SecurityBlock signature;
	private final Map<String, String> attributes;

	DataKind(Builder builder) {
		kindId = builder.kindId;
		name = IANA.nameOf(kindId);
		accessPolicy = builder.accessPolicy;
		dataModel = builder.dataModel;
		signature = builder.signature;
		attributes = builder.attributes;
	}

	/**
	 * @return the kind-id associated to this data kind
	 */
	public long getKindId() {
		return kindId;
	}

	/**
	 * @return the kind name, the name can be undefined
	 */
	public String getName() {
		return name;
	}

	/**
	 * Create a data specifier used to fetch data from the overlay
	 * 
	 * @param modelSpecifier
	 *            the data model specifier used to query for a specific data
	 *            value
	 * @return the specifier for this kind
	 * @see ReloadOverlay#fetchData(net.sf.jReload.overlay.ResourceID,
	 *      StoredDataSpecifier...)
	 */
	public StoredDataSpecifier newDataSpecifier(ModelSpecifier<? extends DataValue> modelSpecifier) {
		return new StoredDataSpecifier(this, modelSpecifier);
	}

	/**
	 * @return Get the data model for the data type stored by this kind
	 */
	public DataModel<? extends DataValue> getDataModel() {
		return dataModel;
	}

	/**
	 * Get a parameter generator for the associated access control policy to be
	 * used with the specified overlay
	 */
	public AccessPolicyParamsGenerator getPolicyParamsGenerator(ReloadOverlay overlay) {
		return accessPolicy.getParamsGenerator(overlay.getConfiguration());
	}

	public Map<String, String> getAttributes() {
		return Collections.unmodifiableMap(attributes);
	}

	public String getAttribute(String key) {
		String v = attributes.get(key.toLowerCase());

		if (v != null)
			return v;

		return "";
	}

	public boolean isAttribute(String key) {
		return attributes.containsKey(key.toLowerCase());
	}

	public SecurityBlock getSignature() {
		return signature;
	}

	/**
	 * @return The access control policy associated with this data kind
	 */
	public AccessPolicy getAccessPolicy() {
		return accessPolicy;
	}

	@Override
	public String toString() {
		return "DataKind [kindId=" + kindId + ", dataModel=" + dataModel + ", accessPolicy=" + accessPolicy + ", attributes=" + attributes + "]";
	}

	public static class Builder {

		public static final Class<SingleValue> TYPE_SINGLE = SingleValue.class;
		public static final Class<ArrayValue> TYPE_ARRAY = ArrayValue.class;
		public static final Class<DictionaryValue> TYPE_DICTIONARY = DictionaryValue.class;

		private final long kindId;
		private DataModel<? extends DataValue> dataModel;
		private AccessPolicy accessPolicy;
		private SecurityBlock signature;
		private final Map<String, String> attributes = new HashMap<String, String>();

		public Builder(long kindId) {
			this.kindId = kindId;
		}

		public long getKindId() {
			return kindId;
		}

		public DataModel<? extends DataValue> getDataModel() {
			return dataModel;
		}

		public Builder dataModel(String name) {
			dataModel = DataModel.getInstance(name);
			return this;
		}

		public AccessPolicy getAccessPolicy() {
			return accessPolicy;
		}

		public Builder accessPolicy(AccessPolicy accessPolicy) {
			this.accessPolicy = accessPolicy;
			return this;
		}

		public Builder signature(SecurityBlock kindSignature) {
			signature = kindSignature;
			return this;
		}

		public Object attribute(String key, String value) {
			return attributes.put(key.toLowerCase(), value);
		}

		/**
		 * Check kind definition constraints and build the data kind
		 * 
		 * @throws ConfigurationException
		 *             if some kind constraint was not fullfilled
		 */
		public DataKind build() throws ConfigurationException {
			checkParams();
			return new DataKind(this);
		}

		private void checkParams() throws ConfigurationException {
			if (accessPolicy == null)
				throw new ConfigurationException("Access policy not set");
			if (dataModel == null)
				throw new ConfigurationException("Data model not set");

			accessPolicy.checkKindParams(this);
		}

		public String getAttribute(String key) {
			String v = attributes.get(key.toLowerCase());

			if (v != null)
				return v;

			return "";
		}

		public int getIntAttribute(String key) {
			try {
				return Integer.parseInt(getAttribute(key));
			} catch (NumberFormatException e) {
				return 0;
			}
		}

		public long getLongAttribute(String key) {
			try {
				return Long.parseLong(getAttribute(key));
			} catch (NumberFormatException e) {
				return 0;
			}
		}
	}
}
