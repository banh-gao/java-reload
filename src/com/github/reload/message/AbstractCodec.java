package com.github.reload.message;

import com.github.reload.Context;

public abstract class AbstractCodec<T> implements Codec<T> {

	@Override
	public void init(Context ctx, CodecFactory factory) {
	}
}
