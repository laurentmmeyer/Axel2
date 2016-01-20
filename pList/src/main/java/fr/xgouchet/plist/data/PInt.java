package fr.xgouchet.plist.data;

public class PInt extends PObject {

	public PInt(long value) {
		mType = Type.INT;
		mValue = value;
	}

	public long getValue() {
		return mValue;
	}

	public String toString() {
		return "INT (" + mValue + ")";
	}

	private long mValue;
}
