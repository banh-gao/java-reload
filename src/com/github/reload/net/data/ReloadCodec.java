package com.github.reload.net.data;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates the codec to be used for RELOAD encoding of the associated class.
 * 
 * @see Codec
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ReloadCodec {

	Class<? extends Codec<?>> value();
}
