package com.github.reload.message;

import java.util.Arrays;
import com.github.reload.message.SignerIdentity.IdentityType;
import net.sf.jReload.message.DecodingException;
import net.sf.jReload.message.EncUtils;
import net.sf.jReload.message.UnsignedByteBuffer;

/**
 * The identity value of the signer
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public abstract class SignerIdentityValue {

	private final HashAlgorithm certHashAlg;

	protected abstract void implWriteTo(UnsignedByteBuffer buf);

	public SignerIdentityValue(HashAlgorithm certHashAlg) {
		this.certHashAlg = certHashAlg;
	}

	public static SignerIdentityValue parse(UnsignedByteBuffer buf, int length, IdentityType identityType) {

		HashAlgorithm certHashAlg = HashAlgorithm.valueOf(buf.getRaw8());
		if (certHashAlg == null)
			throw new DecodingException("Unsupported hash algorithm");

		SignerIdentityValue value = null;

		switch (identityType) {
			case NONE :
				value = new NoneSignerIndentityValue();
				break;
			case CERT_HASH :
				value = new CertHashSignerIdentityValue(buf, certHashAlg);
				break;
			case CERT_HASH_NODE_ID :
				value = new CertHashNodeIdSignerIdentityValue(buf, certHashAlg);
				break;
		}

		assert (value != null);

		return value;
	}

	public final void writeTo(UnsignedByteBuffer buf) {
		buf.putRaw8(certHashAlg.getCode());
		implWriteTo(buf);
	}

	public abstract byte[] getHashValue();

	public HashAlgorithm getCertHashAlg() {
		return certHashAlg;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(getHashValue());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SignerIdentityValue other = (SignerIdentityValue) obj;
		if (!Arrays.equals(getHashValue(), other.getHashValue()))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SignerIdentityValue[certHashAlg=" + certHashAlg + " hash=" + EncUtils.toHexString(getHashValue()) + "]";
	}
}