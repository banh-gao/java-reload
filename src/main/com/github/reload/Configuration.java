package com.github.reload;

import java.util.HashMap;
import java.util.Map;

public class Configuration {

	public static AttributeKey<Integer> NODE_ID_LENGTH = AttributeKey.newKey();

	private Map<AttributeKey<?>, Object> overlayAttributes = new HashMap<AttributeKey<?>, Object>();

	@SuppressWarnings("unchecked")
	public <T> T getOverlayAttribute(AttributeKey<T> key) {
		if (!overlayAttributes.containsKey(key))
			throw new IllegalStateException("No configuration attribute for the specified key");
		return (T) overlayAttributes.get(key);
	}

	@SuppressWarnings("unchecked")
	public <T> T setOverlayAttribute(AttributeKey<T> key, T value) {
		return (T) overlayAttributes.put(key, value);
	}

	public static final class AttributeKey<T> {

		public static <T> AttributeKey<T> newKey() {
			return new AttributeKey<T>();
		}
	}
}
