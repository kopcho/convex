package convex.core.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bouncycastle.util.Arrays;
import org.junit.jupiter.api.Test;

import convex.core.data.Blob;
import convex.core.data.Format;
import convex.core.data.Ref;
import convex.core.data.Tag;
import convex.core.util.Utils;

public class HashTest {
	public static final String GENESIS_HEADER = "0100000000000000000000000000000000000000000000000000000000000000000000003ba3edfd7a7b12b27ac72c3e67768f617fc81bc3888a51323a9fb8aa4b1e5e4a29ab5f49ffff001d1dac2b7c";

	@Test
	public void testBasicSHA256() {
		// empty bytes
		Hash h1 = Hash.sha256(Utils.EMPTY_BYTES);
		assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", h1.toHexString());

		// 32 empty bytes
		Hash h1_32 = Hash.sha256(new byte[32]);
		assertEquals("66687aadf862bd776c8fc18b8e9f8e20089714856ee233b3902a591d0d5f2925", h1_32.toHexString());

		// Hash from https://www.freeformatter.com/sha256-generator.html
		Hash h2 = Hash.sha256("Hello");
		assertEquals("185f8db32271fe25f561a6fc938b2e264306ec304eda518007d1764826381969", h2.toHexString());

		// Hash from https://passwordsgenerator.net/sha256-hash-generator/
		Hash h3 = Hash.sha256("Boo");
		assertEquals("BF66F3E41E470B7D073DB8C5FB82E737962D63080EDBCC4F9EF3C3CE735472EA",
				h3.toHexString().toUpperCase());
	}

	@Test
	public void testBasicKeccak256() {
		// empty bytes Keccak256
		// note that SHA-3 is
		// "a7ffc6f8bf1ed76651c14756a061d662f580ff4de43b49fa82d80a4b80f8434a"
		Hash h1 = Hash.keccak256(Utils.EMPTY_BYTES);
		assertEquals("c5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470", h1.toHexString());

		// from https://leventozturk.com/engineering/sha3/
		Hash h2 = Hash.keccak256("abc");
		assertEquals("4e03657aea45a94fc7d47ba826c8d667c0d1e6e33a64a036ec44f58fa12d6c45", h2.toHexString());

		// from web3j test suite
		Hash h3 = Hash.keccak256("EVWithdraw(address,uint256,bytes32)");
		assertEquals("953d0c27f84a9649b0e121099ffa9aeb7ed83e65eaed41d3627f895790c72d41", h3.toHexString());

		// Random name hash
		Hash h4 = Hash.keccak256("Poland");
		assertEquals("62812097841d5ae3dd00c7fbe3157fad0b8118c15b2f5816f6831069f6056f82", h4.toHexString());
	}
	
	@Test 
	public void testBuiltinHashes() {
		Hash h1 = Hash.sha3(Utils.EMPTY_BYTES);
		assertEquals("a7ffc6f8bf1ed76651c14756a061d662f580ff4de43b49fa82d80a4b80f8434a", h1.toHexString());
		assertEquals(Hash.EMPTY_HASH, h1);
		
	}

	@Test
	public void testNullHash() {
		// hash of single zero byte (CVM null encoding), tested against multiple online calculators
		assertEquals("5d53469f20fef4f8eab52b88044ede69c77a6a68a60728609fc4a65ff531e7d0", Hash.NULL_HASH.toHexString());
		
		// different ways of getting the same result, should all correspond
		assertSame(Hash.NULL_HASH, Hash.compute(null));
		assertSame(Hash.NULL_HASH, Ref.create(null).getHash());
	}

	@Test
	public void testDataLength() {
		assertEquals(33, Hash.NULL_HASH.encodedLength());
	}

	@Test
	public void testExtractHash() {
		Hash h = Hash.compute("foo");
		assertTrue(h.isCanonical());
		Blob b = Format.encodedBlob(h);
		byte[] bs = b.getBytes();
		Hash h2 = Hash.wrap(bs, 1, Hash.LENGTH); // all bytes except the initial tag byte
		assertEquals(h, h2);
	}

	@Test
	public void testBitcoinGenesis() {
		// Bitcoin genesis block header
		// 0100000000000000000000000000000000000000000000000000000000000000000000003ba3edfd7a7b12b27ac72c3e67768f617fc81bc3888a51323a9fb8aa4b1e5e4a29ab5f49ffff001d1dac2b7c
		// Should hash to:
		// 000000000019d6689c085ae165831e934ff763ae46a2a6c172b3f1b60a8ce26
		// after double hashing and interpretation in little-endian format
		byte[] genesisHeader = Utils.hexToBytes(GENESIS_HEADER);
		assertEquals(80, genesisHeader.length);
		// genesisHeader=Arrays.reverse(genesisHeader);
		Hash h1 = Hash.sha256(genesisHeader);
		assertEquals("af42031e805ff493a07341e2f74ff58149d22ab9ba19f61343e2c86c71c5d66d", h1.toHexString());
		Hash h2 = h1.computeHash(Hash.getSHA256Digest());
		assertEquals("6fe28c0ab6f1b372c1a6a246ae63f74f931e8365e15a089c68d6190000000000", h2.toHexString());
		// reversed bytes for little-endian format
		assertEquals("000000000019d6689c085ae165831e934ff763ae46a2a6c172b3f1b60a8ce26f",
				Utils.toHexString(Arrays.reverse(h2.getBytes())));
	}

	@Test
	public void testEquality() {
		Hash h = Hash.NULL_HASH;
		assertEquals(h, Hash.sha3(new byte[] { Tag.NULL }));
		assertNotEquals(h, h.toBlob());

		assertEquals(0, h.compareTo(h.toBlob()));
	}
}
