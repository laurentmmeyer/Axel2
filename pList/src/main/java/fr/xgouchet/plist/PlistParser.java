package fr.xgouchet.plist;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.NotSerializableException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;

import android.util.Base64;
import android.util.Log;
import fr.xgouchet.plist.data.PArray;
import fr.xgouchet.plist.data.PBoolean;
import fr.xgouchet.plist.data.PData;
import fr.xgouchet.plist.data.PDate;
import fr.xgouchet.plist.data.PDict;
import fr.xgouchet.plist.data.PInt;
import fr.xgouchet.plist.data.PNull;
import fr.xgouchet.plist.data.PObject;
import fr.xgouchet.plist.data.PReal;
import fr.xgouchet.plist.data.PString;

public class PlistParser {

	public static final String TAG = "PlistP";

	private static final int TRAILER_SIZE = 0x20;

	private static final int MARKER_NULL = 0x00;
	private static final int MARKER_FALSE = 0x08;
	private static final int MARKER_TRUE = 0x09;
	private static final int MARKER_FILL = 0x0F;

	private static final int TYPE_NBF = 0x00;
	private static final int TYPE_INT = 0x10;
	private static final int TYPE_REAL = 0x20;
	private static final int TYPE_DATE = 0x30;
	private static final int TYPE_DATA = 0x40;
	private static final int TYPE_ASCII = 0x50;
	private static final int TYPE_UNICODE = 0x60;
	private static final int TYPE_UUID = 0x80;
	private static final int TYPE_ARRAY = 0xA0;
	private static final int TYPE_SET = 0xC0;
	private static final int TYPE_DICT = 0xD0;

	private final static long EPOCH = 978307200000L;

	public PlistParser() {
	}

	/**
	 * Parses the xml data in the given file,
	 * 
	 * @param source
	 *            the source file to parse
	 * @param listener
	 *            the listener for XML events (must not be null)
	 * @throws IOException
	 *             if the input can't be read
	 */
	public PObject parse(final InputStream input)
			throws NotSerializableException, EOFException, IOException {
		// TODO is.available may not be accurate !!!
		mData = new byte[input.available()];
		input.read(mData);
		input.close();

		return parseBinaryPlist();

	}

	private PObject parseBinaryPlist() throws EOFException,
			NotSerializableException {
		readHeader();
		readTrailer();
		readOffsetTable();

		return readPObject(mRootObject);
	}

	/**
	 * Reads the Plist header
	 */
	private void readHeader() throws NotSerializableException {
		String bplist = new String(mData, 0, 6);
		String version = new String(mData, 6, 2);

		if (!"bplist".equalsIgnoreCase(bplist)) {
			throw new NotSerializableException(
					"File is not a binary property list : wrong header");
		}

		Log.i(TAG, "Plist version " + version);
		mParserOffset = 8;
	}

	/**
	 * Reads the plist trailer :
	 * <ul>
	 * <li>6 nul bytes</li>
	 * <li>1 unsigned byte offest size</li>
	 * <li>1 unsigned byte object ref size</li>
	 * <li>4 nul bytes</li>
	 * <li>4 bytes : number of objects</li>
	 * <li>4 nul bytes</li>
	 * <li>4 bytes : root object</li>
	 * <li>4 nul bytes</li>
	 * <li>4 bytes : table offset</li>
	 */
	private void readTrailer() throws EOFException {
		if (mData.length < 32) {
			throw new EOFException(
					"File is truncated or corrupted : missing trailer");
		}

		byte[] trailer = new byte[32];
		int i, trailerOffset;

		trailerOffset = mData.length - TRAILER_SIZE;
		for (i = 0; i < TRAILER_SIZE; ++i) {
			trailer[i] = mData[trailerOffset + i];
		}

		mOffsetSize = trailer[6] & 0xFF;
		mObjectRefSize = trailer[7] & 0xFF;
		mObjectCount = mRootObject = mOffsetTableOffset = 0;

		for (i = 0; i < 4; ++i) {
			mObjectCount <<= 8;
			mRootObject <<= 8;
			mOffsetTableOffset <<= 8;
			mObjectCount |= trailer[12 + i] & 0xFF;
			mRootObject |= trailer[20 + i] & 0xFF;
			mOffsetTableOffset |= trailer[28 + i] & 0xFF;
		}
	}

	/**
	 * Reads the plist offset table
	 */
	private void readOffsetTable() throws EOFException {
		if (mData.length < (mOffsetTableOffset + (mObjectCount * mOffsetSize))) {
			throw new EOFException(
					"File is truncated or corrupted : missing offset table");
		}

		mOffsetTable = new int[mObjectCount];

		int i, j, index, value;
		for (i = 0; i < mObjectCount; ++i) {
			value = 0;
			index = mOffsetTableOffset + (i * mOffsetSize);
			for (j = 0; j < mOffsetSize; j++) {
				value <<= 8;
				value |= mData[index + j] & 0xFF;
			}
			mOffsetTable[i] = value;
		}
	}

	/**
	 * @param ref
	 *            the reference of the object to read
	 * @return the object read
	 */
	private PObject readPObject(int ref) {
		mParserOffset = mOffsetTable[ref];
		return readPobject();
	}

	/**
	 * @return the object read at the current offset
	 */
	private PObject readPobject() {
		PObject object;

		final int marker = readMarker();
		final int type = (marker & 0xF0);

		switch (type) {
		case TYPE_NBF:
			object = readNBF(marker);
			break;
		case TYPE_INT:
			object = readInt(marker);
			break;
		case TYPE_REAL:
			object = readReal(marker);
			break;
		case TYPE_DATE:
			object = readDate(marker);
			break;
		case TYPE_DATA:
			object = readData(marker);
			break;
		case TYPE_ASCII:
			object = readASCII(marker);
			break;
		case TYPE_UNICODE:
			object = readUnicode(marker);
			break;
		case TYPE_ARRAY:
		case TYPE_SET:
			object = readArray(marker);
			break;
		case TYPE_DICT:
			object = readDict(marker);
			break;
		default:
			object = new PObject();
			mParserOffset++;
			Log.w(TAG, "Unknown marker 0x" + Integer.toHexString(marker));
			break;
		}

		return object;
	}

	/**
	 * @return the next marker value
	 */
	private int readMarker() {
		int marker = (mData[mParserOffset] & 0xFF);
		mParserOffset++;
		return marker;
	}

	/**
	 * Reads a Fixed object (null, boolean or Fill)
	 * 
	 * @param marker
	 */
	private PObject readNBF(final int marker) {
		PObject object;

		switch (marker) {
		case MARKER_NULL:
		case MARKER_FILL:
			object = PNull.NULL;
			break;
		case MARKER_TRUE:
			object = PBoolean.TRUE;
			break;
		case MARKER_FALSE:
			object = PBoolean.FALSE;
			break;
		default:
			object = new PObject();
			Log.w(TAG, "Unknown NBF marker 0x" + Integer.toHexString(marker));
			break;
		}

		return object;
	}

	/**
	 * Reads an integer value
	 * 
	 * @param marker
	 */
	private PInt readInt(final int marker) {
		int count = 1 << (marker & 0XF);
		long value = 0;
		if (count <= 8) {
			for (int i = 0; i < count; ++i) {
				value <<= 8;
				value |= mData[mParserOffset] & 0xFF;
				mParserOffset++;
			}
		} else {
			byte[] bytes = new byte[count];
			for (int i = 0; i < count; ++i) {
				bytes[i] = (byte) mData[mParserOffset];
				mParserOffset++;
			}

			BigInteger big = new BigInteger(bytes);
			value = big.longValue();
		}

		return new PInt(value);
	}

	/**
	 * Reads a real value
	 * 
	 * @param marker
	 */
	private PObject readReal(final int marker) {
		int count = 1 << (marker & 0XF);

		long value = 0;
		for (int i = 0; i < count; ++i) {
			value <<= 8;
			value |= mData[mParserOffset] & 0xFF;
			mParserOffset++;
		}

		return new PReal(Float.intBitsToFloat((int) value));
	}

	/**
	 * 
	 */
	private PDate readDate(final int marker) {
		int count = 1 << (marker & 0XF);

		long value = 0;
		for (int i = 0; i < count; ++i) {
			value <<= 8;
			value |= mData[mParserOffset] & 0xFF;
			mParserOffset++;
		}

		double asDouble = 0;
		if (count == 8) {
			asDouble = Double.longBitsToDouble(value);
		} else if (count == 4) {
			asDouble = Float.intBitsToFloat((int) value);
		} else {
			throw new IllegalArgumentException("Cannot convert " + count
					+ " bytes to date");
		}

		return new PDate(EPOCH + (long) (1000 * asDouble));
	}

	/**
	 * Reads an ASCII string
	 * 
	 * @param marker
	 * @return
	 */
	private PString readASCII(final int marker) {
		int count;

		// Get character count
		count = (marker & 0xF);
		if (count == 0xF) {
			count = (int) readInt(readMarker()).getValue();
		}

		// read characters
		byte[] bytes = new byte[count];
		for (int i = 0; i < count; i++) {
			bytes[i] = (byte) mData[mParserOffset];
			mParserOffset++;
		}

		String value = null;
		try {
			value = new String(bytes, "US-ASCII");
		} catch (UnsupportedEncodingException e) {
			value = new String(bytes);
		}

		return new PString(value);
	}

	/**
	 * Reads an ASCII string
	 * 
	 * @param marker
	 * @return
	 */
	private PString readUnicode(final int marker) {
		int count;

		// Get character count
		count = (marker & 0xF);
		if (count == 0xF) {
			count = (int) readInt(readMarker()).getValue();
		}

		count *= 2; // 2 bytes per char

		// read characters
		byte[] bytes = new byte[count];
		for (int i = 0; i < count; i++) {
			bytes[i] = (byte) mData[mParserOffset];
			mParserOffset++;
		}

		String value = null;
		try {
			value = new String(bytes, "UTF-16BE");
		} catch (UnsupportedEncodingException e) {
			value = new String(bytes);
		}

		return new PString(value);
	}

	private PData readData(int marker) {
		int count;

		// Get character count
		count = (marker & 0xF);
		if (count == 0xF) {
			count = (int) readInt(readMarker()).getValue();
		}

		// read characters
		byte[] bytes = new byte[count];
		for (int i = 0; i < count; i++) {
			bytes[i] = (byte) mData[mParserOffset];
			mParserOffset++;
		}

		return new PData(Base64.encodeToString(bytes, Base64.DEFAULT));
	}

	/**
	 * @param marker
	 * @return
	 */
	private PArray readArray(final int marker) {
		int count;

		// Get object count
		count = (marker & 0xF);
		if (count == 0xF) {
			count = (int) readInt(readMarker()).getValue();
		}

		PArray array = new PArray(count);

		int[] refs = new int[count];

		// read objects' refs
		for (int i = 0; i < count; i++) {
			refs[i] = readObjectRef();
		}

		for (int i = 0; i < count; i++) {
			array.setObject(i, readPObject(refs[i]));
		}

		return array;
	}

	/**
	 * 
	 */
	private PDict readDict(int marker) {
		int count;

		// Get map count
		count = (marker & 0xF);
		if (count == 0xF) {
			count = (int) readInt(readMarker()).getValue();
		}

		PDict dict = new PDict(count);

		int keys[], refs[];

		// read keys refs
		keys = new int[count];
		for (int i = 0; i < count; i++) {
			keys[i] = readObjectRef();
		}

		// read objects' refs
		refs = new int[count];
		for (int i = 0; i < count; i++) {
			refs[i] = readObjectRef();
		}

		// read real objects
		PObject key, value;
		for (int i = 0; i < count; i++) {
			key = readPObject(keys[i]);
			value = readPObject(refs[i]);
			dict.setEntry(key, value);
		}

		return dict;
	}

	/**
	 * @return the next read object ref
	 */
	private int readObjectRef() {
		int i, ref = 0;

		for (i = 0; i < mObjectRefSize; ++i) {
			ref <<= 8;
			ref |= (mData[mParserOffset] & 0xFF);
			mParserOffset++;
		}

		return ref;
	}

	// Internal
	private byte[] mData;
	private int mParserOffset;
	private int mOffsetSize, mObjectRefSize, mObjectCount, mRootObject,
			mOffsetTableOffset;
	private int[] mOffsetTable;

}
