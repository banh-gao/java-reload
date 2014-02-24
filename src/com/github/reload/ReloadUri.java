package com.github.reload;

import java.net.URI;
import java.net.URISyntaxException;
import javax.security.auth.login.Configuration;
import com.github.reload.message.DestinationList;
import com.github.reload.message.RoutableID;
import com.github.reload.storage.errors.UnknownKindException;

/**
 * A RELOAD Uniform Resource Identifier defined as:
 * reload://hexDestinationList@overlay/[dataSpecifier]
 * 
 */
public class ReloadUri {

	private static final String SCHEME = "reload";

	private static final String OVERLAY_NAME_PATTEN = "[a-zA-Z0-9._%-=]+";

	private static final int DEST_BUF_SIZE = 3000;

	private final DestinationList destinationList;
	private final String overlayName;
	private final DataSpecifier specifier;

	private ReloadUri(DestinationList destList, String overlayName, DataSpecifier specifier) {
		destinationList = destList;
		this.overlayName = overlayName;
		this.specifier = specifier;
	}

	/**
	 * Build a RELOAD URI from the specified values
	 * 
	 * @param destList
	 *            the uri destination list, must contain at least one
	 *            destination-id
	 * @param overlayName
	 *            a valid overlay name
	 * 
	 * @throws NullPointerException
	 *             if the destination list or the overlay name is null
	 * @throws IllegalArgumentException
	 *             if the destination list is empty or the overlay name is not
	 *             valid
	 */
	public static ReloadUri create(DestinationList destList, String overlayName) {
		return create(destList, overlayName, null);
	}

	/**
	 * Build a RELOAD URI from the specified values
	 * 
	 * @param destList
	 *            the uri destination list, must contain at least one
	 *            destination-id
	 * @param overlayName
	 *            a valid overlay name
	 * @param dataSpecifier
	 *            the data specified to associate to the uri, if null the URI
	 *            will not contain the data specifier part
	 * 
	 * @throws NullPointerException
	 *             if the destination list or the overlay name is null
	 * @throws IllegalArgumentException
	 *             if the destination list is empty or the overlay name is not
	 *             valid
	 */
	public static ReloadUri create(DestinationList destList, String overlayName, DataSpecifier dataSpecifier) {
		if (destList == null || overlayName == null)
			throw new NullPointerException();
		if (destList.isEmpty())
			throw new IllegalArgumentException("Empty destination list");
		if (!overlayName.matches(OVERLAY_NAME_PATTEN))
			throw new IllegalArgumentException("Invalid overlay name");

		return new ReloadUri(DestinationList.create(destList), overlayName, dataSpecifier);
	}

	/**
	 * Try to parse the passed URI to a RELOAD URI, the eventually data
	 * specifier will not be parsed. See {@link #create(URI, Configuration)} for
	 * data specifier parsing.
	 * 
	 * @param uri
	 *            a Uniform Resource Identifier in the RELOAD format
	 * @throws URISyntaxException
	 *             if the URI is not a valid RELOAD URI
	 * @throws NullPointerException
	 *             if the uri is null
	 */
	public static ReloadUri create(URI uri) throws URISyntaxException {
		try {
			return create(uri, null);
		} catch (UnknownKindException e) {
			throw new AssertionError(e);
		}
	}

	/**
	 * Try to parse the passed string to a RELOAD URI, the eventually data
	 * specifier will not be parsed. See {@link #create(String, Configuration)}
	 * for data specifier parsing.
	 * The RELOAD URI destination list must contain at least one destination-id.
	 * 
	 * @param uri
	 *            a Uniform Resource Identifier in the RELOAD format
	 * @throws URISyntaxException
	 *             if the URI is not a valid RELOAD URI
	 * @throws NullPointerException
	 *             if the uri is null
	 */
	public static ReloadUri create(String uri) throws URISyntaxException {
		return create(URI.create(uri));
	}

	/**
	 * Try to parse the passed URI to a RELOAD URI, the eventually data
	 * specifier will also be parsed using the data kind defined in the
	 * specified configuration. If the configuration doesn't contain the kind
	 * definition for the data specifier of the uri, an UnknownKindException
	 * will be throwed.
	 * 
	 * @param uri
	 *            a Uniform Resource Identifier in the RELOAD format
	 * @param conf
	 *            the configuration to be used to decode the data specifier. If
	 *            null, the data specifier will not be parsed.
	 * @throws URISyntaxException
	 *             if the URI is not a valid RELOAD URI
	 * @throws UnknownKindException
	 *             if the configuration doesn't contain a kind definition for
	 *             the data specifier in the uri
	 * @throws NullPointerException
	 *             if the uri is null
	 * 
	 */
	public static ReloadUri create(URI uri, Configuration conf) throws URISyntaxException, UnknownKindException {
		if (uri == null)
			throw new NullPointerException();

		if (!uri.getScheme().equalsIgnoreCase(SCHEME))
			throw new URISyntaxException(uri.toString(), "Invalid RELOAD URI scheme", 0);

		String overlayName = uri.getHost();
		if (!overlayName.matches(OVERLAY_NAME_PATTEN))
			throw new URISyntaxException(uri.toString(), "Invalid overlay name");

		DestinationList destList = parseDestinationList(uri);
		if (destList.isEmpty())
			throw new IllegalArgumentException("Empty destination list");

		DataSpecifier specifier = (conf != null) ? parseSpecifier(uri, conf) : null;

		return new ReloadUri(destList, overlayName, specifier);
	}

	/**
	 * Try to parse the passed string to a RELOAD URI, the eventually data
	 * specifier will also be parsed using the data kind defined in the
	 * specified configuration. If the configuration doesn't contain the kind
	 * definition for the data specifier of the uri, an UnknownKindException
	 * will be throwed.
	 * 
	 * @param uri
	 *            a Uniform Resource Identifier in the RELOAD format
	 * @param conf
	 *            the configuration to be used to decode the data specifier. If
	 *            null, the data specifier will not be parsed.
	 * @throws URISyntaxException
	 *             if the URI is not a valid RELOAD URI
	 * @throws UnknownKindException
	 *             if the configuration doesn't contain a kind definition for
	 *             the data specifier in the uri
	 * @throws NullPointerException
	 *             if the uri is null
	 * 
	 */
	public static ReloadUri create(String uri, Configuration conf) throws URISyntaxException, UnknownKindException {
		return create(URI.create(uri), conf);
	}

	private static DestinationList parseDestinationList(URI uri) throws URISyntaxException {
		String hexDestList = uri.getUserInfo();

		if (hexDestList == null)
			throw new URISyntaxException(uri.toString(), "Missing destination");

		UnsignedByteBuffer encDestList;
		try {
			encDestList = hexToByte(hexDestList);
		} catch (Exception e) {
			throw new URISyntaxException(uri.toString(), "Invalid destination-id encoding");
		}

		return DestinationList.decode(encDestList, encDestList.remaining());
	}

	private static UnsignedByteBuffer hexToByte(String str) {
		UnsignedByteBuffer bytes = UnsignedByteBuffer.allocate(str.length() / 2);
		for (int i = 0; i < bytes.capacity(); i++) {
			bytes.putRaw8((byte) Integer.parseInt(str.substring(2 * i, 2 * i + 2), 16));
		}
		bytes.rewind();
		return bytes;
	}

	private static DataSpecifier parseSpecifier(URI uri, Configuration conf) throws UnknownKindException {
		String hexSpecifier = uri.getPath();
		UnsignedByteBuffer encSpecifier = hexToByte(hexSpecifier);
		return new DataSpecifier(conf, encSpecifier);
	}

	/**
	 * @return the URI representation of this ReloadURI
	 */
	public URI toURI() {
		try {
			String specPath = (specifier != null) ? '/' + getHexSpecifier() : null;
			return new URI(SCHEME, getHexDestList(), overlayName, -1, specPath, null, null);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	private String getHexDestList() {
		UnsignedByteBuffer buf = UnsignedByteBuffer.allocate(DEST_BUF_SIZE);
		writeDestListTo(buf);
		return byteToHex(buf);
	}

	private void writeDestListTo(UnsignedByteBuffer buf) {
		for (RoutableID id : destinationList) {
			id.writeAsDestinationTo(buf);
		}
	}

	private String getHexSpecifier() {
		if (specifier == null)
			return "";

		UnsignedByteBuffer buf = UnsignedByteBuffer.allocate((int) EncUtils.maxUnsignedInt(DataSpecifier.DATA_SPEC_LENGTH_FIELD));
		specifier.writeTo(buf);

		return byteToHex(buf);
	}

	private static String byteToHex(UnsignedByteBuffer b) {
		StringBuilder hexString = new StringBuilder();

		int length = b.position();

		b.rewind();

		int readed = 0;

		while (readed < length) {
			String stmp = Integer.toHexString(b.getRaw8() & 0XFF);

			if (stmp.length() == 1) {
				hexString.append("0" + stmp);
			} else {
				hexString.append(stmp);
			}
			readed++;
		}
		return hexString.toString();
	}

	/**
	 * @return the destination list contained in this uri
	 */
	public DestinationList getDestinationList() {
		return destinationList;
	}

	/**
	 * @return the overlay name contained in the uri
	 */
	public String getOverlayName() {
		return overlayName;
	}

	/**
	 * @return the data specifier contained in the uri, or null if not present
	 */
	public DataSpecifier getSpecifier() {
		return specifier;
	}

	/**
	 * @see URI#toASCIIString()
	 */
	public String toASCIIString() {
		return toURI().toASCIIString();
	}

	/**
	 * @see URI#toString()
	 */
	@Override
	public String toString() {
		return toURI().toString();
	}

	/**
	 * @see URI#hashCode()
	 */
	@Override
	public int hashCode() {
		return toURI().hashCode();
	}

	/**
	 * @see URI#equals(Object)
	 */
	@Override
	public boolean equals(Object obj) {
		return toURI().equals(obj);
	}
}
