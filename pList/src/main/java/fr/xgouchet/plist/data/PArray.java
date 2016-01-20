package fr.xgouchet.plist.data;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class PArray extends PObject implements Iterable<PObject> {

	/**
	 * 
	 */
	public class PArrayIterator implements Iterator<PObject> {

		public PArrayIterator(PObject[] objects) {
			mObjects = objects;
			mIteratorIndex = 0;
		}

		/**
		 * @see java.util.Iterator#hasNext()
		 */
		public boolean hasNext() {
			return (mIteratorIndex < mObjects.length);
		}

		/**
		 * @see java.util.Iterator#next()
		 */
		public PObject next() {
			if (mIteratorIndex >= mObjects.length) {
				throw new NoSuchElementException("Array index: "
						+ mIteratorIndex);
			}

			PObject object = mObjects[mIteratorIndex];
			mIteratorIndex++;

			return object;
		}

		/**
		 * @see java.util.Iterator#remove()
		 */
		public void remove() {
			throw new UnsupportedOperationException(
					"PArray objects can't be modified");
		}

		private int mIteratorIndex;
		private final PObject[] mObjects;
	}

	/**
	 * @param count
	 *            the number of objects in the array
	 */
	public PArray(int count) {
		mType = Type.ARRAY;
		mObjects = new PObject[count];
	}

	/**
	 * @see java.lang.Iterable#iterator()
	 */
	public Iterator<PObject> iterator() {
		return new PArrayIterator(mObjects);
	}

	/**
	 * 
	 */
	public void setObject(int index, PObject obj) {
		mObjects[index] = obj;
	}

	/**
	 * @see fr.xgouchet.plist.data.PObject#toString()
	 */
	public String toString() {
		return "ARRAY [" + mObjects.length + "]";
	}

	private final PObject[] mObjects;

}
