package org.ironrhino.core.security.jwt;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

public class JwtTest {

	@Test
	public void test() {
		String secret = "iampassword";
		String sub = "admin";
		String jwt = Jwt.createWithSubject(sub, secret);
		assertThat(Jwt.extractSubject(jwt), is(sub));
		Jwt.verifySignature(jwt, secret);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidJwt() {
		Jwt.extractSubject("xxxxxx");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testExpiredJwt() {
		String jwt = Jwt.createWithSubject("admin", "password", -61);
		Jwt.extractSubject(jwt);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidSignature() {
		String jwt = Jwt.createWithSubject("admin", "password");
		Jwt.verifySignature(jwt, "notsamepassword");
	}

}
