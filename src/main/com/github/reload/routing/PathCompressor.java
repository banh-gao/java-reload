package com.github.reload.routing;

import com.github.reload.net.encoders.Header;
import com.github.reload.net.encoders.header.OpaqueID;

/**
 * Compress and decompress header destination lists by replacing it with opaque
 * ids
 * 
 */
public interface PathCompressor {

	/**
	 * Replace part of the header destination list with a compressed path
	 * identified by an opaque-id
	 * 
	 * @param outgoingHeader
	 */
	public void compressDestinationFor(Header outgoingHeader);

	/**
	 * If the first entry of the header destination list is an opaque-id,
	 * replace it with the corresponding decompressed list
	 * 
	 * @throws UnknownOpaqueIdException
	 *             if the first entry of the destination list is an opaque-id
	 *             not known by this manager
	 */
	public void decompressDestinationFor(Header incomingHeader) throws UnknownOpaqueIdException;

	/**
	 * Indicates that the requested opaque-id is not known
	 * 
	 */
	public class UnknownOpaqueIdException extends Exception {

		private final OpaqueID id;

		public UnknownOpaqueIdException(OpaqueID id) {
			super("Unknown path compression id " + id);
			this.id = id;
		}

		public OpaqueID getOpaqueId() {
			return id;
		}
	}
}
