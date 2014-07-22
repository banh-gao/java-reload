package com.github.reload;

import io.netty.util.AttributeKey;
import java.util.Map;
import com.github.reload.Components.Component;
import com.google.common.collect.Maps;

public class NodeInformation implements Component {

	private final Map<AttributeKey<?>, Object> values = Maps.newHashMap();

	@Override
	public void compStart() {
		// TODO Auto-generated method stub
	}

	@SuppressWarnings("unchecked")
	public <T> T get(AttributeKey<T> key) {
		return (T) values.get(key);
	}

}
