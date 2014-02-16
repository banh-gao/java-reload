package com.github.reload.storage;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import javax.naming.ConfigurationException;
import com.github.reload.message.SecurityBlock;
import com.github.reload.storage.DataModel.ModelType;

/**
 * The description of the data kind that a peer can handle
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class DataKind {

	public static final String ATTR_MAX_COUNT = "max-count";
	public static final String ATTR_MAX_SIZE = "max-size";
	public static final String ATTR_MAX_NODE_MULTIPLE = "max-node-multiple";

	/**
	 * The data-kinds registered to IANA
	 * 
	 */
	public enum IANA {
		TURN_SERVICE("TURN-SERVICE", KindId.valueOf(2)),
		CERTIFICATE_BY_NODE("CERTIFICATE_BY_NODE", KindId.valueOf(3)),
		CERTIFICATE_BY_USER("CERTIFICATE_BY_USER", KindId.valueOf(16));

		private final String name;
		private final KindId id;

		private IANA(String name, KindId id) {
			this.name = name;
			this.id = id;
		}

		public KindId getId() {
			return id;
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

		public static String nameOf(KindId kindId) {
			for (IANA kind : EnumSet.allOf(IANA.class))
				if (kind.id.equals(kindId))
					return kind.name;
			return "";
		}
	}

	private final KindId kindId;
	private final String name;
	private final DataModel dataModel;
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
	public KindId getKindId() {
		return kindId;
	}

	/**
	 * @return the kind name, the name can be undefined
	 */
	public String getName() {
		return name;
	}

	/**
	 * Create a data specifier to be used to fetch data from the overlay
	 * 
	 * @return the specifier for this kind
	 * @see ReloadOverlay#fetchData(net.sf.jReload.overlay.ResourceID,
	 *      DataSpecifier...)
	 */
	public DataSpecifier newDataSpecifier() {
		return new DataSpecifier(this, dataModel.newSpecifier());
	}

	/**
	 * Create a data specifier to be used to fetch data from the overlay, uses
	 * the passed value as model specifier
	 * 
	 * @return the specifier for this kind
	 * @see ReloadOverlay#fetchData(net.sf.jReload.overlay.ResourceID,
	 *      DataSpecifier...)
	 * @throws IllegalArgumentException
	 *             if the passed model specifier is not compatible with this
	 *             kind data model
	 */
	public DataSpecifier newDataSpecifier(DataModelSpecifier modelSpecifier) {
		if (modelSpecifier.getModelType() != getDataModel().getModelType())
			throw new IllegalArgumentException("Expected model specifier for type " + getDataModel().getModelType() + ", " + modelSpecifier.getModelType() + " given");
		return new DataSpecifier(this, modelSpecifier);
	}

	/**
	 * Create a prepared data to be used to prepare data that will be stored
	 * into the overlay
	 * 
	 * @return the prepared data for this kind
	 */
	public PreparedData newPreparedData() {
		return new PreparedData(this, dataModel.newPreparedValue(this));
	}

	/**
	 * Create a prepared data to be used to prepare data that will be stored
	 * into the overlay, uses the passed value as the prepared value
	 * 
	 * @return the prepared data for this kind
	 * @throws IllegalArgumentException
	 *             if the passed prepared value is not compatible with this kind
	 *             data model
	 */
	public PreparedData newPreparedData(PreparedValue preparedValue) {
		if (!equals(preparedValue.getDataKind()))
			throw new IllegalArgumentException("Prepared value for kind " + preparedValue.getDataKind().getKindId() + " not compatible with the kind " + getKindId());
		return new PreparedData(this, preparedValue);
	}

	/**
	 * Get a parameter generator for the associated access control policy to be
	 * used with
	 * the specified overlay
	 */
	public AccessPolicyParamsGenerator getPolicyParamsGenerator(ReloadOverlay overlay) {
		return accessPolicy.getParamsGenerator(overlay);
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

	public boolean isAttribute(String key) {
		return attributes.containsKey(key.toLowerCase());
	}

	public SecurityBlock getSignature() {
		return signature;
	}

	DataModelSpecifier parseModelSpecifier(UnsignedByteBuffer buf, int length) {
		return dataModel.parseSpecifier(buf, length);
	}

	DataValue parseValue(UnsignedByteBuffer buf, int length) {
		return dataModel.parseValue(buf, length);
	}

	Metadata parseMetadata(UnsignedByteBuffer buf, int length) {
		return dataModel.parseMetadata(buf, length);
	}

	/**
	 * @return The access control policy associated with this data kind
	 */
	public AccessPolicy getAccessPolicy() {
		return accessPolicy;
	}

	public DataModel getDataModel() {
		return dataModel;
	}

	@Override
	public final int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + kindId.hashCode();
		return result;
	}

	@Override
	public final boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DataKind other = (DataKind) obj;

		return getKindId().equals(other.getKindId());
	}

	@Override
	public String toString() {
		return "DataKind [kindId=" + kindId + ", dataModel=" + dataModel + ", accessPolicy=" + accessPolicy + ", attributes=" + attributes + "]";
	}

	public static class Builder {

		private final KindId kindId;
		private DataModel dataModel;
		private AccessPolicy accessPolicy;
		private SecurityBlock signature;
		private final Map<String, String> attributes = new HashMap<String, String>();

		public Builder(KindId kindId) {
			this.kindId = kindId;
			attribute(ATTR_MAX_COUNT, EncUtils.maxUnsignedInt(EncUtils.U_INT32) + "");
			attribute(ATTR_MAX_SIZE, EncUtils.maxUnsignedInt(EncUtils.U_INT32) + "");
			attribute(ATTR_MAX_NODE_MULTIPLE, "0");
		}

		public KindId getKindId() {
			return kindId;
		}

		public DataModel getDataModel() {
			return dataModel;
		}

		public Builder dataModel(ModelType mod) {
			dataModel = DataModel.getInstance(mod);
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
