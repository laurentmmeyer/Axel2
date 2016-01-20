package fr.xgouchet.plist.data;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class PDict extends PObject implements Iterable<Entry<String, PObject>> {

	/**
	 * 
	 */
	public PDict(int size) {
		mType = Type.DICT;
		mMap = new HashMap<String, PObject>(1 + ((size * 4) / 3));
	}

	/**
	 * @see java.lang.Iterable#iterator()
	 */
	public Iterator<Entry<String, PObject>> iterator() {
		return mMap.entrySet().iterator();
	}

	/**
	 * 
	 */
	public void setEntry(PObject key, PObject value)
			throws IllegalArgumentException {
		if (!PString.class.isAssignableFrom(key.getClass())) {
			throw new IllegalArgumentException(
					"Dictionary keys must be strings");
		}

		PString strKey = (PString) key;
		mMap.put(strKey.getValue(), value);
	}

	/**
	 * @see fr.xgouchet.plist.data.PObject#toString()
	 */
	public String toString() {
		return "DICT";
	}

	private final Map<String, PObject> mMap;
}
