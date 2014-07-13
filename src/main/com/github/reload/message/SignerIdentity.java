package com.github.reload.message;

import io.netty.buffer.ByteBuf;
import java.security.cert.Certificate;
import java.util.EnumSet;
import com.github.reload.Configuration;
import com.github.reload.message.Codec.ReloadCodec;
import com.github.reload.message.SignerIdentity.SignerIdentityCodec;

@ReloadCodec(SignerIdentityCodec.class)
public class SignerIdentity {

	public static final SignerIdentity EMPTY_IDENTITY = new SignerIdentity(IdentityType.NONE, new NoneSignerIndentityValue());

	public enum IdentityType {
		CERT_HASH((byte) 0x01, CertHashSignerIdentityValue.class),
		CERT_HASH_NODE_ID((byte) 0x02, CertHashNodeIdSignerIdentityValue.class),
		NONE((byte) 0x03, NoneSignerIndentityValue.class);

		private final byte value;
		private final Class<? extends SignerIdentityValue> valueClass;

		private IdentityType(byte value, Class<? extends SignerIdentityValue> valueClass) {
			this.value = value;
			this.valueClass = valueClass;
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

		public Class<? extends SignerIdentityValue> getValueClass() {
			return valueClass;
		}
	}

	private final IdentityType identityType;
	private final SignerIdentityValue signerIdentityValue;

	private SignerIdentity(IdentityType idType, SignerIdentityValue idValue) {
		identityType = idType;
		signerIdentityValue = idValue;
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

	public static class SignerIdentityCodec extends Codec<SignerIdentity> {

		private final static int VALUE_LENGTH_FIELD = U_INT16;

		private final Codec<SignerIdentityValue> identityValueCodec;

		public SignerIdentityCodec(Configuration conf) {
			super(conf);
			identityValueCodec = getCodec(SignerIdentityValue.class);
		}

		@Override
		public void encode(SignerIdentity obj, ByteBuf buf, Object... params) throws CodecException {
			buf.writeByte(obj.identityType.value);

			Field lenFld = allocateField(buf, VALUE_LENGTH_FIELD);

			identityValueCodec.encode(obj.signerIdentityValue, buf);

			lenFld.updateDataLength();
		}

		@Override
		public SignerIdentity decode(ByteBuf buf, Object... params) throws CodecException {
			IdentityType idType = IdentityType.valueOf(buf.readByte());
			if (idType == null)
				throw new CodecException("Unsupported identity type");

			ByteBuf identityData = readField(buf, VALUE_LENGTH_FIELD);

			SignerIdentityValue idValue = identityValueCodec.decode(identityData, idType);

			identityData.release();

			return new SignerIdentity(idType, idValue);
		}

	}
}
