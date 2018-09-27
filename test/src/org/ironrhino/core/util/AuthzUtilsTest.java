package org.ironrhino.core.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuthzUtilsTest {

	@Test
	public void testAuthorizeAccess() throws Exception {
		Collection<String> roles = Arrays.asList("ROLE_A", "ROLE_B", "ROLE_C");
		Authentication auth = new TestingAuthenticationToken("admin", "password",
				roles.toArray(new String[roles.size()]));
		SecurityContextHolder.getContext().setAuthentication(auth);
		assertTrue(AuthzUtils.authorize("permitAll"));
		assertFalse(AuthzUtils.authorize("denyAll"));
		assertTrue(AuthzUtils.authorize("hasRole('ROLE_A')"));
		assertTrue(AuthzUtils.authorize("hasRole('ROLE_A') && hasRole('ROLE_B')"));
		assertTrue(AuthzUtils.authorize("hasRole('ROLE_A') && hasRole('ROLE_B') && hasRole('ROLE_C')"));
		assertFalse(AuthzUtils.authorize("hasRole('ROLE_A') && !hasRole('ROLE_B')"));

		assertTrue(AuthzUtils.authorizeRoles(roles, "hasRole('ROLE_A')"));
		assertTrue(AuthzUtils.authorizeRoles(roles, "hasRole('ROLE_A') && hasRole('ROLE_B')"));
		assertTrue(AuthzUtils.authorizeRoles(roles, "hasRole('ROLE_A') && hasRole('ROLE_B') && hasRole('ROLE_C')"));
		assertFalse(AuthzUtils.authorizeRoles(roles, "hasRole('ROLE_A') && !hasRole('ROLE_B')"));
	}

	@Test
	public void testAuthorizeRoles() throws Exception {
		List<String> roles = Arrays.asList("A,B,C".split(","));
		List<String> ifAllGranted = Arrays.asList("A,B,C".split(","));
		List<String> ifAnyGranted = Arrays.asList("C,D".split(","));
		List<String> ifNotGranted = Arrays.asList("D".split(","));
		assertTrue(AuthzUtils.authorizeRoles(roles, String.join(",", ifAllGranted), null, null));
		assertTrue(AuthzUtils.authorizeRoles(roles, null, String.join(",", ifAnyGranted), null));
		assertTrue(AuthzUtils.authorizeRoles(roles, null, null, String.join(",", ifNotGranted)));
		assertTrue(AuthzUtils.authorizeRoles(roles, ifAllGranted.toArray(new String[0]), null, null));
		assertTrue(AuthzUtils.authorizeRoles(roles, null, ifAnyGranted.toArray(new String[0]), null));
		assertTrue(AuthzUtils.authorizeRoles(roles, null, null, ifNotGranted.toArray(new String[0])));
		ifAllGranted = Arrays.asList("A,B,C,D".split(","));
		ifAnyGranted = Arrays.asList("D,E".split(","));
		ifNotGranted = Arrays.asList("C,D".split(","));
		assertFalse(AuthzUtils.authorizeRoles(roles, String.join(",", ifAllGranted), null, null));
		assertFalse(AuthzUtils.authorizeRoles(roles, null, String.join(",", ifAnyGranted), null));
		assertFalse(AuthzUtils.authorizeRoles(roles, null, null, String.join(",", ifNotGranted)));
		assertFalse(AuthzUtils.authorizeRoles(roles, ifAllGranted.toArray(new String[0]), null, null));
		assertFalse(AuthzUtils.authorizeRoles(roles, null, ifAnyGranted.toArray(new String[0]), null));
		assertFalse(AuthzUtils.authorizeRoles(roles, null, null, ifNotGranted.toArray(new String[0])));
		ifAllGranted = Arrays.asList("A,B C".split("\\s"));
		ifAnyGranted = Arrays.asList("C,D".split("\\s"));
		ifNotGranted = Arrays.asList("D,E".split("\\s"));
		assertTrue(AuthzUtils.authorizeRoles(roles, ifAllGranted.toArray(new String[0]), null, null));
		assertTrue(AuthzUtils.authorizeRoles(roles, null, ifAnyGranted.toArray(new String[0]), null));
		assertTrue(AuthzUtils.authorizeRoles(roles, null, null, ifNotGranted.toArray(new String[0])));
		ifAllGranted = Arrays.asList("A,B C,D".split("\\s"));
		ifAnyGranted = Arrays.asList("D,E".split("\\s"));
		ifNotGranted = Arrays.asList("C,D".split("\\s"));
		assertFalse(AuthzUtils.authorizeRoles(roles, ifAllGranted.toArray(new String[0]), null, null));
		assertFalse(AuthzUtils.authorizeRoles(roles, null, ifAnyGranted.toArray(new String[0]), null));
		assertFalse(AuthzUtils.authorizeRoles(roles, null, null, ifNotGranted.toArray(new String[0])));
	}

}
