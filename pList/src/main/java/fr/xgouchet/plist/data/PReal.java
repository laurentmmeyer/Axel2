package fr.xgouchet.plist.data;

public class PReal extends PObject {

	public PReal(double value) {
		mValue = value;
		mType = Type.REAL;
	}

	public double getValue() {
		return mValue;
	}

	public String toString() {
		return "REAL (" + mValue + ")";
	}

	private double mValue;
}
