package fr.xgouchet.plist.data;


public final class PBoolean extends PObject {

	private PBoolean(boolean value) {
		mValue = value;
		mType = Type.BOOL;
	}
	
	public boolean isTrue() {
		return mValue;
	}

	public String toString() {
		return mValue ? "TRUE" : "FALSE";
	}

	public static final PBoolean TRUE = new PBoolean(true);
	public static final PBoolean FALSE = new PBoolean(false);
	private boolean mValue;
}
