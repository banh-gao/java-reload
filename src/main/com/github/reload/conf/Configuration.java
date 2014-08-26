package com.github.reload.conf;

import io.netty.util.AttributeKey;
import java.net.InetSocketAddress;
import java.security.cert.Certificate;
import java.util.List;
import java.util.Set;
import com.github.reload.net.ice.HostCandidate.OverlayLinkType;
import com.github.reload.services.storage.DataKind;

public interface Configuration {

	public static final AttributeKey<String> OVERLAY_NAME = AttributeKey.valueOf("overlayName");
	public static final AttributeKey<List<? extends Certificate>> ROOT_CERTS = AttributeKey.valueOf("rootCerts");
	public static final AttributeKey<Short> INITIAL_TTL = AttributeKey.valueOf("initialTTL");
	public static final AttributeKey<Integer> NODE_ID_LENGTH = AttributeKey.valueOf("nodeIdLength");
	public static final AttributeKey<Integer> MAX_MESSAGE_SIZE = AttributeKey.valueOf("maxMessageSize");
	public static final AttributeKey<Set<DataKind>> DATA_KINDS = AttributeKey.valueOf("dataKinds");
	public static final AttributeKey<Set<InetSocketAddress>> BOOT_NODES = AttributeKey.valueOf("bootNodes");
	public static final AttributeKey<Set<String>> LINK_PROTOCOLS = AttributeKey.valueOf("linkProtocols");
	public static final AttributeKey<Set<OverlayLinkType>> LINK_TYPES = AttributeKey.valueOf("linkTypes");
	public static final AttributeKey<Boolean> NO_ICE = AttributeKey.valueOf("noIce");

	public <T> T get(AttributeKey<T> name);

	public <T> T set(AttributeKey<T> name, T value);
}