package com.github.reload.message;

import java.security.GeneralSecurityException;
import java.security.cert.Certificate;
import java.util.EnumSet;
import net.sf.jReload.message.DecodingException;
import net.sf.jReload.message.EncUtils;
import net.sf.jReload.message.NodeID;
import net.sf.jReload.message.UnsignedByteBuffer;
import net.sf.jReload.message.UnsignedByteBuffer.Field;

/**
 * The identity of the signer of the message
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class SignerIdentity {

	public enum IdentityType {
		CERT_HASH((byte) 0x01),
		CERT_HASH_NODE_ID((byte) 0x02),
		NONE((byte) 0x03);

		private final byte value;

		private IdentityType(byte value) {
			this.value = value;
		}

		public static IdentityType valueOf(byte b) {
			for (IdentityType id : EnumSet.allOf(IdentityType.class))
				if (id.value == b)
					return id;
			return null;
		}

		public byte value() {
			return value;
		}
	}

	private final static int VALUE_LENGTH_FIELD = EncUtils.U_INT16;

	public final static int MAX_LENGTH = EncUtils.U_INT8 + VALUE_LENGTH_FIELD + (int) EncUtils.maxUnsignedInt(VALUE_LENGTH_FIELD);

	public final static SignerIdentity EMPTY_IDENTITY = new SignerIdentity(IdentityType.NONE, new NoneSignerIndentityValue());

	private final IdentityType identityType;
	private final SignerIdentityValue signerIdentityValue;

	private SignerIdentity(IdentityType idType, SignerIdentityValue idValue) {
		identityType = idType;
		signerIdentityValue = idValue;
	}

	public static SignerIdentity parse(UnsignedByteBuffer buf) {
		IdentityType identityType = IdentityType.valueOf(buf.getRaw8());
		if (identityType == null)
			throw new DecodingException("Unsupported identity type");

		int identityLength = buf.getLengthValue(VALUE_LENGTH_FIELD);

		SignerIdentityValue idValue = SignerIdentityValue.parse(buf, identityLength, identityType);

		if (identityType == IdentityType.NONE)
			return EMPTY_IDENTITY;

		return new SignerIdentity(identityType, idValue);
	}

	/**
	 * Identity for peers that uses a single node-id
	 */
	public static SignerIdentity singleIdIdentity(HashAlgorithm certHashAlg, Certificate signerCertificate) {
		return new SignerIdentity(IdentityType.CERT_HASH, new CertHashSignerIdentityValue(certHashAlg, signerCertificate));
	}

	/**
	 * Identity for peers that uses multiple node-ids the id to specify is the
	 * one used to sign the message
	 */
	public static SignerIdentity multipleIdIdentity(HashAlgorithm certHashAlg, Certificate signerCertificate, NodeID signerNodeId) {
		return new SignerIdentity(IdentityType.CERT_HASH_NODE_ID, new CertHashNodeIdSignerIdentityValue(certHashAlg, signerCertificate, signerNodeId));
	}

	public IdentityType getIdentityType() {
		return identityType;
	}

	public SignerIdentityValue getSignerIdentityValue() {
		return signerIdentityValue;
	}

	public void writeTo(UnsignedByteBuffer buf) {
		buf.putRaw8(identityType.value());

		Field lenFld = buf.allocateLengthField(VALUE_LENGTH_FIELD);

		signerIdentityValue.writeTo(buf);

		lenFld.setEncodedLength(buf.getConsumedFrom(lenFld.getNextPosition()));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((identityType == null) ? 0 : identityType.hashCode());
		result = prime * result + ((signerIdentityValue == null) ? 0 : signerIdentityValue.hashCode());
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
		SignerIdentity other = (SignerIdentity) obj;
		if (identityType != other.identityType)
			return false;
		if (signerIdentityValue == null) {
			if (other.signerIdentityValue != null)
				return false;
		} else if (!signerIdentityValue.equals(other.signerIdentityValue))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder out = new StringBuilder();
		out.append("SignerIdentity[type=" + identityType + ',');
		out.append("value=" + signerIdentityValue.toString() + ']');
		return out.toString();
	}

	/**
	 * Indicates a problem with the signer identity
	 * 
	 * @author Daniel Zozin <zdenial@gmx.com>
	 * 
	 */
	public static class SignerIdentityException extends GeneralSecurityException {

		public SignerIdentityException(String message) {
			super(message);
		}

	}
}
