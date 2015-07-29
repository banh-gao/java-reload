package com.github.reload.services.storage;

import io.netty.buffer.ByteBuf;
import io.netty.util.AttributeKey;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import com.github.reload.components.ComponentsContext;
import com.github.reload.net.encoders.Codec;
import com.github.reload.net.encoders.Codec.ReloadCodec;
import com.github.reload.services.storage.DataKind.DataKindCodec;
import com.github.reload.services.storage.policies.AccessPolicy;

/**
 * The description of the data kind that a peer can handle
 * 
 */
@ReloadCodec(DataKindCodec.class)
public class DataKind {

	private static final Map<Long, DataKind> REGISTERED_KINDS = new HashMap<Long, DataKind>();

	public static final int MAX_SIZE_DEFAULT = 1024;
	public static final int MAX_COUNT_DEFAULT = 1;
	public static final AttributeKey<Integer> MAX_SIZE = AttributeKey.valueOf("maxSize");
	public static final AttributeKey<Integer> MAX_COUNT = AttributeKey.valueOf("maxCount");
	public static final AttributeKey<Long> MAX_NODE_MULTIPLE = AttributeKey.valueOf("maxNodeMultiple");

	private final long kindId;
	private final DataModel dataModel;
	private final Class<? extends AccessPolicy> accessPolicy;
	private final Map<AttributeKey<?>, Object> attributes;

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
		attributes = new HashMap<AttributeKey<?>, Object>(builder.attributes);
	}

	/**
	 * @return the kind-id associated to this data kind
	 */
	public long getKindId() {
		return kindId;
	}

	/**
	 * @return Get the data model for the data type stored by this kind
	 */
	public DataModel getDataModel() {
		return dataModel;
	}

	public Map<AttributeKey<?>, Object> getAttributes() {
		return Collections.unmodifiableMap(attributes);
	}

	@SuppressWarnings("unchecked")
	public <T> T getAttribute(AttributeKey<T> key) {
		return (T) attributes.get(key);
	}

	public <T> T getAttribute(AttributeKey<T> key, T defaultValue) {
		T value = getAttribute(key);
		if (value == null)
			return defaultValue;
		return value;
	}

	/**
	 * @return The access control policy associated with this data kind
	 */
	public Class<? extends AccessPolicy> getPolicyClass() {
		return accessPolicy;
	}

	@Override
	public String toString() {
		return "DataKind [kindId=" + kindId + ", dataModel=" + dataModel + ", accessPolicy=" + accessPolicy + ", attributes=" + attributes + "]";
	}

	public static class Builder {

		private final long kindId;
		private DataModel dataModel;
		private Class<? extends AccessPolicy> accessPolicy;
		private final Map<AttributeKey<?>, Object> attributes = new HashMap<AttributeKey<?>, Object>();

		public Builder(long kindId) {
			this.kindId = kindId;
		}

		public Builder dataModel(DataModel dataModel) {
			this.dataModel = dataModel;
			return this;
		}

		public <T> Builder attribute(AttributeKey<T> key, T value) {
			attributes.put(key, value);
			return this;
		}

		public Builder accessPolicy(Class<? extends AccessPolicy> accessPolicy) {
			this.accessPolicy = accessPolicy;
			return this;
		}

		public DataKind build() {
			if (dataModel == null || accessPolicy == null)
				throw new IllegalStateException();
			return new DataKind(this);
		}
	}

	static class DataKindCodec extends Codec<DataKind> {

		public DataKindCodec(ComponentsContext ctx) {
			super(ctx);
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
				throw new UnknownKindException(Collections.singletonList(kindId));

			return kind;
		}
	}
}
