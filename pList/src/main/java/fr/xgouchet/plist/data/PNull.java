package fr.xgouchet.plist.data;

public class PNull extends PObject {

	private PNull() {
		mType = Type.NULL;
	}

	public String toString() {
		return "NULL";
	}

	public static final PNull NULL = new PNull();
}
