package com.github.reload.message;

import net.sf.jReload.message.UnsignedByteBuffer;

/**
 * Empty identity value
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
class NoneSignerIndentityValue extends SignerIdentityValue {

	public NoneSignerIndentityValue() {
		super(HashAlgorithm.NONE);
	}

	@Override
	protected void implWriteTo(UnsignedByteBuffer buf) {
		// No data
	}

	@Override
	public byte[] getHashValue() {
		return new byte[0];
	}

}