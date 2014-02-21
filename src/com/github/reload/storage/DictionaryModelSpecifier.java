package com.github.reload.storage;

import java.util.ArrayList;
import java.util.List;
import com.github.reload.storage.DataModel.DataType;
import com.github.reload.storage.data.DictionaryEntry;
import com.github.reload.storage.data.DictionaryEntry.Key;

/**
 * Specifier used to fetch dictionary values
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class DictionaryModelSpecifier extends DataModelSpecifier {

	private static final int KEYS_LENGTH_FIELD = EncUtils.U_INT16;

	List<Key> keys = new ArrayList<DictionaryEntry.Key>();

	DictionaryModelSpecifier() {
		super(DataType.DICTIONARY);
	}

	DictionaryModelSpecifier(UnsignedByteBuffer buf) {
		super(DataType.DICTIONARY);
		int length = buf.getLengthValue(DictionaryModelSpecifier.KEYS_LENGTH_FIELD);

		int startPos = buf.position();

		while (buf.getConsumedFrom(startPos) < length) {
			Key key = new Key(buf);
			keys.add(key);
		}
	}

	public void addKey(Key key) {
		keys.add(key);
	}

	public void addKey(byte[] key) {
		if (key == null)
			throw new NullPointerException();
		addKey(new Key(key));
	}

	public List<Key> getKeys() {
		return keys;
	}

	@Override
	void writeTo(UnsignedByteBuffer buf) {
		Field lenFld = buf.allocateLengthField(KEYS_LENGTH_FIELD);

		for (Key k : keys) {
			k.writeTo(buf);
		}

		lenFld.setEncodedLength(buf.getConsumedFrom(lenFld.getNextPosition()));
	}
}
