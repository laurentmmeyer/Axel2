package fr.xgouchet.plist;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public final class PlistUtils {

	public static boolean isBinaryPlist(final File source) {
		boolean result;

		try {
			final InputStream input = new FileInputStream(source.getPath());
			final byte[] header = new byte[8];

			input.read(header, 0, 8);

			result = true;
			result &= (header[0] == 0x62); // b
			result &= (header[1] == 0x70); // p
			result &= (header[2] == 0x6C); // l
			result &= (header[3] == 0x69); // i
			result &= (header[4] == 0x73); // s
			result &= (header[5] == 0x74); // t
			// The two next bytes are the version number

			input.close();
		} catch (Exception e) {
			result = false;
		}

		return result;
	}

	private PlistUtils() {
	}
}
