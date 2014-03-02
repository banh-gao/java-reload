package com.github.reload;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import java.net.URI;
import java.net.URISyntaxException;
import com.github.reload.message.DestinationList;
import com.github.reload.net.data.Codec;
import com.github.reload.net.data.Codec.CodecException;
import com.github.reload.storage.data.StoredDataSpecifier;
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
	private final StoredDataSpecifier specifier;

	private ReloadUri(DestinationList destList, String overlayName, StoredDataSpecifier specifier) {
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
	public static ReloadUri create(DestinationList destList, String overlayName, StoredDataSpecifier dataSpecifier) {
		if (destList == null || overlayName == null)
			throw new NullPointerException();
		if (destList.isEmpty())
			throw new IllegalArgumentException("Empty destination list");
		if (!overlayName.matches(OVERLAY_NAME_PATTEN))
			throw new IllegalArgumentException("Invalid overlay name");

		return new ReloadUri(new DestinationList(destList), overlayName, dataSpecifier);
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

		DestinationList destList = decodeDestinationList(uri, conf);
		if (destList.isEmpty())
			throw new IllegalArgumentException("Empty destination list");

		StoredDataSpecifier specifier = (conf != null) ? parseSpecifier(uri, conf) : null;

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

	private static DestinationList decodeDestinationList(URI uri, Configuration conf) throws URISyntaxException {
		String hexDestList = uri.getUserInfo();

		if (hexDestList == null)
			throw new URISyntaxException(uri.toString(), "Missing destination");

		try {
			ByteBuf encList = hexToByte(hexDestList);
			Codec<DestinationList> codec = Codec.getCodec(DestinationList.class, conf);
			return codec.decode(encList);
		} catch (Exception e) {
			throw new URISyntaxException(uri.toString(), "Invalid destination-id encoding");
		}
	}

	private static ByteBuf hexToByte(String str) {
		ByteBuf buf = UnpooledByteBufAllocator.DEFAULT.buffer(str.length() / 2);
		int i = 0;
		while (buf.writableBytes() > 0) {
			buf.writeByte(Integer.parseInt(str.substring(2 * i, 2 * i + 2), 16));
			i++;
		}
		return buf;
	}

	private static StoredDataSpecifier parseSpecifier(URI uri, Configuration conf) throws UnknownKindException {
		String hexSpecifier = uri.getPath();
		ByteBuf encSpecifier = hexToByte(hexSpecifier);
		Codec<StoredDataSpecifier> codec = Codec.getCodec(StoredDataSpecifier.class, conf);
		try {
			return codec.decode(encSpecifier);
		} catch (CodecException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * @return the URI representation of this ReloadURI
	 */
	public URI toURI(Configuration conf) {
		try {
			String specPath = (specifier != null) ? '/' + getHexSpecifier(conf) : null;
			return new URI(SCHEME, getHexDestList(conf), overlayName, -1, specPath, null, null);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	private String getHexDestList(Configuration conf) {
		ByteBuf buf = UnpooledByteBufAllocator.DEFAULT.buffer(DEST_BUF_SIZE);
		Codec<DestinationList> codec = Codec.getCodec(DestinationList.class, conf);
		try {
			codec.encode(destinationList, buf);
		} catch (CodecException e) {
			e.printStackTrace();
		}
		return byteToHex(buf);
	}

	private String getHexSpecifier(Configuration conf) {
		if (specifier == null)
			return "";

		ByteBuf buf = UnpooledByteBufAllocator.DEFAULT.buffer(EncUtils.maxUnsignedInt(DataSpecifier.DATA_SPEC_LENGTH_FIELD));

		Codec<StoredDataSpecifier> codec = Codec.getCodec(StoredDataSpecifier.class, conf);
		codec.encode(specifier, buf);

		return byteToHex(buf);
	}

	private static String byteToHex(ByteBuf b) {
		StringBuilder hexString = new StringBuilder();

		while (b.readableBytes() > 0) {
			String stmp = Integer.toHexString(b.readUnsignedByte() & 0XFF);

			if (stmp.length() == 1) {
				hexString.append('0');
				hexString.append(stmp);
			} else {
				hexString.append(stmp);
			}
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
	public StoredDataSpecifier getSpecifier() {
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
