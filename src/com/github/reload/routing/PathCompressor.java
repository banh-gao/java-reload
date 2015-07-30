package com.github.reload.routing;

import com.github.reload.net.codecs.header.DestinationList;
import com.github.reload.net.codecs.header.OpaqueID;

/**
 * Compress and decompress header destination lists by replacing it with opaque
 * ids
 * 
 */
public interface PathCompressor {

	/**
	 * Stores the given destination list locally and returns the opaque-id that
	 * can be used later to decompress it back to the original list
	 */
	public OpaqueID compress(DestinationList list);

	/**
	 * Get a previously stored destination list associated with the given
	 * opaque-id
	 * 
	 * @throws UnknownOpaqueIdException
	 *             if the first entry of the destination list is an opaque-id
	 *             not known by this manager
	 */
	public DestinationList decompress(OpaqueID id) throws UnknownOpaqueIdException;

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
