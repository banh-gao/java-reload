package com.github.reload.conf;

import java.net.InetSocketAddress;
import java.net.URL;
import java.security.cert.Certificate;
import java.util.Date;
import java.util.List;
import java.util.Set;
import com.github.reload.Components.Component;
import com.github.reload.crypto.ReloadCertificate;
import com.github.reload.net.encoders.content.errors.UnknownKindException;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.encoders.secBlock.HashAlgorithm;
import com.github.reload.net.encoders.secBlock.Signature;
import com.github.reload.net.ice.IceCandidate.OverlayLinkType;
import com.github.reload.storage.DataKind;

@Component(Configuration.COMPNAME)
public interface Configuration {

	public static final String COMPNAME = "com.github.reload.conf.Configuration";

	/**
	 * @return The overlay name
	 */
	public abstract String getOverlayName();

	public abstract int getConfigurationSequence();

	public abstract Date getExpiration();

	public abstract String getTopologyPlugin();

	/**
	 * @return the length in bytes of the nodeids used in this overlay
	 */
	public abstract int getNodeIdLength();

	/**
	 * @return the maximum size of a message allowed in the overlay in bytes
	 */
	public abstract int getMaxMessageSize();

	/**
	 * @return the maximum time in milliseconds to wait until a request should
	 *         be considerated unreplied
	 */
	public abstract int getReliabilityTimer();

	/**
	 * @return the initial value of the time to live field of the forwarding
	 *         header
	 */
	public abstract short getInitialTTL();

	public abstract DataKind getDataKind(long kindId) throws UnknownKindException;

	public abstract Set<Long> getDataKindIds();

	public abstract List<NodeID> getKindSigners();

	public abstract Set<InetSocketAddress> getBootstrapNodes();

	public abstract List<URL> getEnrollmentServers();

	public abstract List<? extends Certificate> getRootCerts();

	public abstract boolean admitSelfSigned();

	public abstract List<NodeID> getBadNodes();

	public abstract List<String> getMandatoryExtensions();

	public abstract List<String> getLinkProtocols();

	public abstract short getTurnDensity();

	public abstract HashAlgorithm getSelfSignedDigestType();

	public abstract String getSharedSecret();

	public abstract boolean isClientPermitted();

	public abstract boolean isNoICE();

	public abstract Set<OverlayLinkType> getOverlayLinkTypes();

	public abstract List<NodeID> getConfSigners();

	public abstract boolean isSelfSignedPermitted();

	public abstract byte[] getRawXML();

	public abstract Signature getSignature();

	public abstract ReloadCertificate getRootCertificate();

	public abstract String toString();

}