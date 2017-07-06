package org.ironrhino.core.spring.security.password;

import org.ironrhino.core.util.CodecUtils;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.crypto.keygen.BytesKeyGenerator;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.util.EncodingUtils;

public class MixedPasswordEncoder implements PasswordEncoder {

	private final BytesKeyGenerator saltGenerator = KeyGenerators.secureRandom();

	@Override
	public String encode(CharSequence rawPassword) {
		// legacy password
		// return CodecUtils.digest(rawPassword.toString());
		byte[] digest = digest(rawPassword, saltGenerator.generateKey());
		return new String(Hex.encode(digest));
	}

	@Override
	public boolean matches(CharSequence rawPassword, String encodedPassword) {
		if (rawPassword == null || encodedPassword == null)
			return false;
		if (encodedPassword.length() > 32) {
			byte[] digested = Hex.decode(encodedPassword);
			byte[] salt = EncodingUtils.subArray(digested, 0, saltGenerator.getKeyLength());
			return matches(digested, digest(rawPassword, salt));
		} else {
			// legacy password
			return encodedPassword.equals(CodecUtils.digest(rawPassword.toString()));
		}
	}

	private byte[] digest(CharSequence rawPassword, byte[] salt) {
		String digested = CodecUtils.digest(rawPassword.toString(), new String(Hex.encode(salt)));
		byte[] digest = EncodingUtils.concatenate(new byte[][] { salt, Hex.decode(digested) });
		return digest;
	}

	private boolean matches(byte[] expected, byte[] actual) {
		if (expected.length != actual.length) {
			return false;
		}
		int result = 0;
		for (int i = 0; i < expected.length; i++) {
			result |= expected[i] ^ actual[i];
		}
		return result == 0;
	}

}