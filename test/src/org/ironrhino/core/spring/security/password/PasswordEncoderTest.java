package org.ironrhino.core.spring.security.password;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.ironrhino.core.util.CodecUtils;
import org.junit.Test;

public class PasswordEncoderTest {

	@Test
	public void testMixedPasswordEncoder() {
		MixedPasswordEncoder pe = new MixedPasswordEncoder();
		String shaPassword = CodecUtils.shaHex("password");
		String value1 = pe.encode("password");
		assertThat(pe.matches("password", value1), is(true));
		assertThat(pe.matches(shaPassword, value1), is(true));
		String value2 = pe.encode("password");
		assertThat(pe.matches("password", value2), is(true));
		assertThat(pe.matches(shaPassword, value2), is(true));
		assertThat(!value1.equals(value2), is(true));
		assertThat(pe.matches("password", "fb6603c6be5733bef5d208a1d6721b84"), is(true));
		assertThat(pe.matches(CodecUtils.shaHex("password"), "fb6603c6be5733bef5d208a1d6721b84"), is(true));
	}

}
