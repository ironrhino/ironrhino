package org.ironrhino.core.security.jwt;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.Duration;

import org.junit.Test;

public class JwtTest {

	@Test
	public void test() {
		String secret = "iampassword";
		String sub = "admin";
		String jwt = Jwt.createWithSubject(sub, null, secret);
		assertThat(Jwt.extractSubject(jwt), is(sub));
		Jwt.verifySignature(jwt, secret);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidJwt() {
		Jwt.extractSubject("xxxxxx");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testExpiredJwt() {
		String jwt = Jwt.createWithSubject("admin", Duration.ofSeconds(-61), "password");
		Jwt.extractSubject(jwt);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidSignature() {
		String jwt = Jwt.createWithSubject("admin", null, "password");
		Jwt.verifySignature(jwt, "notsamepassword");
	}

}
