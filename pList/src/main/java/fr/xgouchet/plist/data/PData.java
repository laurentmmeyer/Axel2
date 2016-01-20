package fr.xgouchet.plist.data;

public class PData extends PObject {

	public PData(String data) {
		mData = data;
		mType = Type.DATA;
	}

	public String getData() {
		return mData;
	}

	private String mData;
}
