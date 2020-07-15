package convex.core.data;

import java.nio.ByteBuffer;
import java.security.MessageDigest;

import convex.core.crypto.Hash;
import convex.core.exceptions.InvalidDataException;
import convex.core.util.Utils;

/**
 * Abstract base class for data objects containing immutable chunks of binary
 * data. Representation is equivalent to a fixed size immutable byte sequence.
 * 
 * Rationale: - Allow data to be encapsulated as an immutable object - Provide
 * specialised methods for processing byte data - Provide a cached Hash value,
 * lazily computed on demand
 * 
 */
public abstract class ABlob extends ACell implements Comparable<ABlob> {
	/**
	 * Cached hash of the Blob data
	 */
	protected Hash storedHash = null;

	/**
	 * Copies the bytes from this blob to a given destination
	 * 
	 * @param dest
	 * @param destOffset
	 */
	public abstract void getBytes(byte[] dest, int destOffset);

	/**
	 * Gets the length of this data object
	 * 
	 * @return The length in bytes of this data object
	 */
	public abstract long length();

	/**
	 * Converts this data object to a lowercase hex string representation
	 */
	public abstract String toHexString();

	/**
	 * Converts this blob to a readable byte buffer
	 */
	public ByteBuffer toByteBuffer() {
		return ByteBuffer.wrap(getBytes());
	}

	/**
	 * Converts this data object to a hex string representation of the given length.
	 * Equivalent to truncating the full String representation.
	 */
	public String toHexString(int length) {
		return toHexString().substring(0, length);
	}

	/**
	 * Gets a contiguous slice of this blob, as a new Blob.
	 * 
	 * Shares underlying backing data where possible
	 * 
	 * @param start  Start position for the created slice
	 * @param length Length of the slice
	 * @return A blob of the specified length, representing a slice of this blob.
	 */
	public abstract ABlob slice(long start, long length);

	/**
	 * Gets a slice of this blob, as a new blob, starting from the given offset and
	 * extending to the end of the blob.
	 * 
	 * Shares underlying backing data where possible. Returned Blob may not be the
	 * same type as the original Blob
	 */
	public ABlob slice(long start) {
		return slice(start, length() - start);
	}

	/**
	 * Converts this object to a Blob instance
	 * 
	 * @return A Blob instance containing the same data as this Blob.
	 */
	public abstract Blob toBlob();

	/**
	 * Computes the length of the longest common hex prefix between two blobs
	 * 
	 * @return The length of the longest common prefix in hex digits
	 */
	public abstract long commonHexPrefixLength(ABlob b);

	/**
	 * Computes the hash of the byte data stored in this Blob, using the default MessageDigest.
	 * 
	 * This is the correct hash ID for a data value if this blob contains the data value's encoding
	 * 
	 * @return The Hash
	 */
	public final Hash getContentHash() {
		if (storedHash == null) {
			storedHash = computeHash(Hash.getDigest());
		}
		return storedHash;
	}

	/**
	 * Computes the hash of the byte data stored in this Blob, using the given MessageDigest.
	 * 
	 * @return The hash
	 */
	public final Hash computeHash(MessageDigest digest) {
		updateDigest(digest);
		return Hash.wrap(digest.digest());
	}

	protected abstract void updateDigest(MessageDigest digest);

	/**
	 * Gets the byte at the specified position in this data object
	 * 
	 * @param i Index of the byte to get
	 * @return The byte at the specified position
	 */
	public abstract byte get(long i);

	/**
	 * Gets the specified hex digit from this data object
	 * 
	 * @param digitPos The position of the hex digit
	 * @return The value of the hex digit, in the range 0-15 inclusive
	 */
	public int getHexDigit(long digitPos) {
		byte b = get(digitPos >> 1);
		if ((digitPos & 1) == 0) {
			return (b >> 4) & 0x0F; // first hex digit
		} else {
			return b & 0x0F; // second hex digit
		}
	}

	/**
	 * Gets a byte array containing a copy of this data object.
	 * 
	 * @return A new byte array containing the contents of this blob.
	 */
	public byte[] getBytes() {
		byte[] result = new byte[Utils.checkedInt(length())];
		getBytes(result, 0);
		return result;
	}

	/**
	 * Append an additional data object to this, creating a new data object.
	 * 
	 * @param d
	 * @return A new blob, containing the additional data appended to this blob.
	 */
	public abstract ABlob append(ABlob d);

	/**
	 * Abstract hashCode used to force subtypes to implement a meaningful Java hash
	 * function
	 * 
	 * Must be consistent with equals, but otherwise can have arbitrary
	 * implementations.
	 */
	@Override
	public abstract int hashCode();

	/**
	 * Determines if this Blob is equal to another Object.
	 * 
	 * Blobs are defined to be equal if they have the same on-chain representation,
	 * i.e. if and only if all of the following are true:
	 * 
	 * - Blob is of the same general type - Blobs are of the same length - All byte
	 * values are equal
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof ABlob) return equals((ABlob) o);
		return false;
	}

	/**
	 * Compares this blob to another blob, in lexographic order soring by first
	 * bytes
	 */
	@Override
	public int compareTo(ABlob b) {
		if (this == b) return 0;
		long alength = this.length();
		long blength = b.length();
		long compareLength = Math.min(alength, blength);
		for (long i = 0; i < compareLength; i++) {
			int c = (0xFF & get(i)) - (0xFF & b.get(i));
			if (c > 0) return 1;
			if (c < 0) return -1;
		}
		if (alength > compareLength) return 1; // this is bigger
		if (blength > compareLength) return -1; // b is bigger
		return 0;
	}

	/**
	 * Determines if this Blob is equal to another Blob.
	 * 
	 * Blobs are defined to be equal if they have the same on-chain representation,
	 * i.e. if and only if all of the following are true:
	 * 
	 * - Blob is of the same general type - Blobs are of the same length - All byte
	 * values are equal
	 * 
	 * @param a
	 * @return True if the blobs are equal, false otherwise.
	 */
	public abstract boolean equals(ABlob a);

	/**
	 * Writes the byte contents of this data object to a ByteBuffer.
	 * 
	 * @param b
	 * @return The passed ByteBuffer, after writing byte content
	 */
	@Override
	public abstract ByteBuffer writeRaw(ByteBuffer bb);

	/**
	 * Gets a chunk of this Blob, as a canonical Blob up to the maximum chunk size
	 * 
	 * @param i Index of chunk
	 * @return A Blob containing the specified chunk data.
	 */
	public abstract Blob getChunk(long i);

	@Override
	public void ednString(StringBuilder sb) {
		sb.append("#blob \"");
		toHexString(sb);
		sb.append("\"");
	}

	/**
	 * Gets a byte buffer containing this Blob's data. Will have remaining bytes
	 * equal to this Blob's size.
	 * 
	 * @return A ByteBuffer containing the Blob's data.
	 */
	public abstract ByteBuffer getByteBuffer();

	public abstract void toHexString(StringBuilder sb);

	@Override
	public void validate() throws InvalidDataException {
		super.validate();
	}

	@Override
	public void validateCell() throws InvalidDataException {
		if (length() < 0) throw new InvalidDataException("Negative blob length", this);
	}

	/**
	 * Returns the number of matching hex digits in the given hex range. Assumes
	 * range is valid for both blobs.
	 * 
	 * Returns length if this Blob is exactly equal to the specified hex range.
	 * 
	 * @param key
	 * @return The number of matching hex characters
	 */
	public abstract long hexMatch(ABlob b, long start, long length);

	public boolean hexEquals(ABlob b) {
		long c = length();
		if (b.length() != c) return false;
		return hexMatch(b, 0L, c) == c;
	}

	public boolean hexEquals(ABlob b, long start, long length) {
		return hexMatch(b, start, length) == length;
	}

	public long hexLength() {
		return length() << 1;
	}

	/**
	 * Gets the long value of this Blob if the length is exactly 8 bytes, otherwise
	 * throws an Exception
	 * 
	 * @return The long value represented by the Blob
	 */
	public abstract long longValue();
}