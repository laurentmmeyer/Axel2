package fr.xgouchet.plist.data;

public class PObject {

	public enum Type {
		NULL, BOOL, INT, REAL, DATE, DATA, STRING, DICT, ARRAY, UNKNOWN
	}

	public PObject() {
		mType = Type.UNKNOWN;
	}

	public Type getType() {
		return mType;
	}

	protected Type mType;

	public String toString() {
		return "<PObject type=\"" + mType + "\"/>";
	}
}
