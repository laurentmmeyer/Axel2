package fr.xgouchet.plist.data;

public class PString extends PObject {

	public PString(String value) {
		mValue = value;
		mType = Type.STRING;
	}

	public String getValue() {
		return mValue;
	}

	public String toString() {
		return "STRING (\"" + mValue + "\")";
	}

	private String mValue;
}
