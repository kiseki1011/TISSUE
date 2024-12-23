package com.tissue.api.util;

/**
 * Provides Base62 encoding.
 * <p>
 * The Base62 alphabet used by this algorithm in common is equivalent to the Base64 alphabet as defined by RFC 2045.
 * The only exception is a representations for 62 and 63 6-bit values. For that values special encoding is used.
 *
 * @author Pavel Myasnov
 */
public class Base62Encoder {
	/**
	 * This array is a lookup table that translates 6-bit positive integer index values into their "Base62 Alphabet"
	 * equivalents as specified in Table 1 of RFC 2045 excepting special characters for 62 and 63 values.
	 * <p>
	 * Thanks to "commons" project in ws.apache.org for this code.
	 * http://svn.apache.org/repos/asf/webservices/commons/trunk/modules/util/
	 */
	private static final char[] ENCODE_TABLE = {
		'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
		'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',
		'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
		'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
	};

	/**
	 * Special mask for the data that should be written in compact 5-bits form
	 */
	private static final int COMPACT_MASK = 0x1E; // 00011110

	/**
	 * Mask for extracting 5 bits of the data
	 */
	private static final int MASK_5BITS = 0x1F; // 00011111

	/**
	 * Encodes binary data using a Base62 algorithm.
	 *
	 * @param data binary data to encode
	 * @return String containing Base62 characters
	 */
	public static String encode(byte[] data) {
		// Reserving capacity for the worst case when each output character represents compacted 5-bits data
		final StringBuilder sb = new StringBuilder(data.length * 8 / 5 + 1);

		final BitInputStream in = new BitInputStream(data);
		while (in.hasMore()) {
			// Read not greater than 6 bits from the stream
			final int rawBits = in.readBits(6);

			// For some cases special processing is needed, so _bits_ will contain final data representation needed to
			// form next output character
			final int bits;
			if ((rawBits & COMPACT_MASK) == COMPACT_MASK) {
				// We can't represent all 6 bits of the data, so extract only least significant 5 bits and return for
				// one bit back in the stream
				bits = rawBits & MASK_5BITS;
				in.seekBit(-1);
			} else {
				// In most cases all 6 bits used to form output character
				bits = rawBits;
			}

			// Look up next character in the encoding table and append it to the output StringBuilder
			sb.append(ENCODE_TABLE[bits]);
		}

		return sb.toString();
	}

	private static class BitInputStream {
		private final byte[] buffer;
		private int offset = 0;

		public BitInputStream(byte[] bytes) {
			this.buffer = bytes;
		}

		public void seekBit(int pos) {
			offset += pos;
			if (offset < 0 || offset > buffer.length * 8) {
				throw new IndexOutOfBoundsException();
			}
		}

		public int readBits(int bitsCount) {
			if (bitsCount < 0 || bitsCount > 7) {
				throw new IndexOutOfBoundsException();
			}

			final int bitNum = offset % 8;
			final int byteNum = offset / 8;

			final int firstRead = Math.min(8 - bitNum, bitsCount);
			final int secondRead = bitsCount - firstRead;

			int result = (buffer[byteNum] & (((1 << firstRead) - 1) << bitNum)) >>> bitNum;
			if (secondRead > 0 && byteNum + 1 < buffer.length) {
				result |= (buffer[byteNum + 1] & ((1 << secondRead) - 1)) << firstRead;
			}

			offset += bitsCount;

			return result;
		}

		public boolean hasMore() {
			return offset < buffer.length * 8;
		}
	}
}
