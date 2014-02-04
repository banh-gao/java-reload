package com.github.reload.message;

import java.util.HashMap;
import java.util.Map;
import com.github.reload.Context;

/**
 * Factory class to get object codecs associated to class representing part of
 * RELOAD messages
 */
public class CodecFactory {

	private final Context ctx;
	private static final Map<Class<? extends Object>, Codec<? extends Object>> codecs = new HashMap<Class<? extends Object>, Codec<? extends Object>>();

	/**
	 * Register a codec for the specified class to the codec factory, the old
	 * codec for the same class, if exists will be returned
	 * 
	 * @param clazz
	 * @param codec
	 */
	@SuppressWarnings("unchecked")
	public static <T> Codec<T> registerCodec(Class<T> clazz, Codec<T> codec) {
		// Safe cast because type is checked when codec was registered
		return (Codec<T>) codecs.put(clazz, codec);
	}

	/**
	 * Get a codec factory for the given context.
	 * Note that this works as long as only one CodecFactory per VM is used.
	 * This is required because because the context is passed to the codecs when
	 * they are retrived from the factory, so using the same codec instance with
	 * different contextes will lead to unpredictable results.
	 * Anyway this limitation may change in the future.
	 * 
	 * @param ctx
	 * @return
	 */
	public static CodecFactory getInstance(Context ctx) {
		return new CodecFactory(ctx);
	}

	private CodecFactory(Context ctx) {
		this.ctx = ctx;
	}

	/**
	 * Get the codec associated to the given class
	 * 
	 * @param clazz
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> Codec<T> getCodec(Class<T> clazz) {
		Codec<?> codec = codecs.get(clazz);
		if (codec == null)
			throw new IllegalStateException("Codec for class " + clazz.getCanonicalName() + " not found");

		codec.init(ctx, this);
		// Safe cast because type is checked when codec was registered
		return (Codec<T>) codec;
	}
}
