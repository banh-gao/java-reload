package com.github.reload.storage;

import io.netty.buffer.ByteBuf;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import com.github.reload.Configuration;
import com.github.reload.ReloadOverlay;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.net.encoders.content.storage.ArrayValue;
import com.github.reload.net.encoders.content.storage.DictionaryValue;
import com.github.reload.net.encoders.content.storage.SingleValue;
import com.github.reload.net.encoders.content.storage.StoredDataSpecifier;
import com.github.reload.storage.AccessPolicy.AccessPolicyParamsGenerator;
import com.github.reload.storage.DataKind.DataKindCodec;
import com.github.reload.storage.DataModel.DataValue;
import com.github.reload.storage.DataModel.ModelSpecifier;

/**
 * The description of the data kind that a peer can handle
 * 
 */
@ReloadCodec(DataKindCodec.class)
public class DataKind {

	private static final Map<Long, DataKind> REGISTERED_KINDS = new HashMap<Long, DataKind>();

	private final long kindId;
	private final int maxCount;
	private final int maxSize;
	private final DataModel<? extends DataValue> dataModel;
	private final AccessPolicy accessPolicy;
	private final Map<String, String> attributes;

	public static DataKind getInstance(long kindId) {
		return REGISTERED_KINDS.get(kindId);
	}

	public static void registerDataKind(DataKind kind) {
		REGISTERED_KINDS.put(kind.kindId, kind);
	}

	DataKind(Builder builder) {
		kindId = builder.kindId;
		accessPolicy = builder.accessPolicy;
		dataModel = builder.dataModel;
		attributes = new HashMap<String, String>(builder.attributes);
		maxCount = builder.maxCount;
		maxSize = builder.maxSize;
	}

	/**
	 * @return the kind-id associated to this data kind
	 */
	public long getKindId() {
		return kindId;
	}

	public int getMaxCount() {
		return maxCount;
	}

	public int getMaxSize() {
		return maxSize;
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

	public boolean hasAttribute(String key) {
		return attributes.containsKey(key.toLowerCase());
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
		public int maxCount;
		public int maxSize;
		private DataModel<? extends DataValue> dataModel;
		private AccessPolicy accessPolicy;
		private final Map<String, String> attributes = new HashMap<String, String>();

		public Builder(long kindId) {
			this.kindId = kindId;
		}

		public Builder dataModel(DataModel<? extends DataValue> dataModel) {
			this.dataModel = dataModel;
			return this;
		}

		public Builder maxCount(int maxCount) {
			this.maxCount = maxCount;
			return this;
		}

		public Builder maxSize(int maxSize) {
			this.maxSize = maxSize;
			return this;
		}

		public Builder accessPolicy(AccessPolicy accessPolicy) {
			this.accessPolicy = accessPolicy;
			return this;
		}

		public Builder attribute(String key, String value) {
			attributes.put(key.toLowerCase(), value);
			return this;
		}

		public DataKind build() {
			checkParams();
			return new DataKind(this);
		}

		private void checkParams() {
			if (maxCount <= 0 || maxSize <= 0 || dataModel == null || accessPolicy == null)
				throw new IllegalStateException();

			accessPolicy.checkKindParams(this);
		}
	}

	public static class DataKindCodec extends Codec<DataKind> {

		public DataKindCodec(Configuration conf) {
			super(conf);
		}

		@Override
		public void encode(DataKind obj, ByteBuf buf, Object... params) throws CodecException {
			buf.writeInt((int) obj.kindId);
		}

		@Override
		public DataKind decode(ByteBuf buf, Object... params) throws CodecException {
			long kindId = buf.readUnsignedInt();

			DataKind kind = DataKind.getInstance(kindId);
			if (kind == null)
				throw new CodecException("Unknown data kind");

			return kind;
		}
	}
}
