package org.ironrhino.core.spring.security.password;

import static org.junit.Assert.assertTrue;

import org.ironrhino.core.util.CodecUtils;
import org.junit.Test;

public class PasswordEncoderTest {

	@Test
	public void testMixedPasswordEncoder() {
		MixedPasswordEncoder pe = new MixedPasswordEncoder();
		String shaPassword = CodecUtils.shaHex("password");
		String value1 = pe.encode("password");
		assertTrue(pe.matches("password", value1));
		assertTrue(pe.matches(shaPassword, value1));
		String value2 = pe.encode("password");
		assertTrue(pe.matches("password", value2));
		assertTrue(pe.matches(shaPassword, value2));
		assertTrue(!value1.equals(value2));
		assertTrue(pe.matches("password", "fb6603c6be5733bef5d208a1d6721b84")); // legacy password
		assertTrue(pe.matches(CodecUtils.shaHex("password"), "fb6603c6be5733bef5d208a1d6721b84")); // legacy password
	}

}
