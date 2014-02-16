package com.github.reload.storage;

import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import com.github.reload.storage.DataResponse.ResponseData;

/**
 * An answer to a storage resource query (includes fetch and stat answer)
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public abstract class QueryAnswer<T extends DataResponse<? extends ResponseData>> extends MessageContent {

	private static final Logger logger = Logger.getLogger(ReloadOverlay.class);

	private static final int RESPONSES_LENGTH_FIELD = EncUtils.U_INT32;

	private final List<T> responses;

	private final Set<Certificate> neededCerts;

	public QueryAnswer(Context context, List<T> responses) {
		this.responses = responses;
		neededCerts = new HashSet<Certificate>();

		for (T r : responses) {
			Iterator<? extends ResponseData> i = r.iterator();

			while (i.hasNext()) {
				ResponseData d = i.next();
				if (!d.getSignerIdentity().equals(SignerIdentity.EMPTY_IDENTITY)) {
					ReloadCertificate c = context.getCryptoHelper().getCertificate(d.getSignerIdentity());
					if (c != null) {
						neededCerts.add(c.getOriginalCertificate());
					} else {
						logger.log(Priority.WARN, "Certificate for stored resource not found: " + d.getSignerIdentity());
						i.remove();
					}
				}
			}
		}
	}

	public QueryAnswer(Context context, UnsignedByteBuffer buf) throws UnknownKindException {
		responses = decodedResponses(context, buf);
		neededCerts = Collections.emptySet();
	}

	@Override
	public Set<? extends Certificate> getNeededCertificates() {
		return neededCerts;
	}

	private List<T> decodedResponses(Context context, UnsignedByteBuffer buf) throws UnknownKindException {
		int length = buf.getLengthValue(QueryAnswer.RESPONSES_LENGTH_FIELD);

		List<T> out = new ArrayList<T>();

		int startPos = buf.position();

		while (buf.getConsumedFrom(startPos) < length) {
			T resp = decodeResponse(context, buf);
			out.add(resp);
		}
		return out;
	}

	public List<T> getResponses() {
		return responses;
	}

	protected abstract T decodeResponse(Context context, UnsignedByteBuffer buf) throws UnknownKindException;

	@Override
	protected void implWriteTo(UnsignedByteBuffer buf) {
		Field lenFld = buf.allocateLengthField(RESPONSES_LENGTH_FIELD);

		for (T r : responses) {
			r.writeTo(buf);
		}

		lenFld.setEncodedLength(buf.getConsumedFrom(lenFld.getNextPosition()));
	}
}
