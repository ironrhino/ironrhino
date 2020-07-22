package org.ironrhino.core.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

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
		assertThat(AuthzUtils.authorize("permitAll"), equalTo(true));
		assertThat(AuthzUtils.authorize("denyAll"), equalTo(false));
		assertThat(AuthzUtils.authorize("hasRole('ROLE_A')"), equalTo(true));
		assertThat(AuthzUtils.authorize("hasRole('ROLE_A') && hasRole('ROLE_B')"), equalTo(true));
		assertThat(AuthzUtils.authorize("hasRole('ROLE_A') && hasRole('ROLE_B') && hasRole('ROLE_C')"), equalTo(true));
		assertThat(AuthzUtils.authorize("hasRole('ROLE_A') && !hasRole('ROLE_B')"), equalTo(false));

		assertThat(AuthzUtils.authorizeRoles(roles, "hasRole('ROLE_A')"), equalTo(true));
		assertThat(AuthzUtils.authorizeRoles(roles, "hasRole('ROLE_A') && hasRole('ROLE_B')"), equalTo(true));
		assertThat(AuthzUtils.authorizeRoles(roles, "hasRole('ROLE_A') && hasRole('ROLE_B') && hasRole('ROLE_C')"),
				equalTo(true));
		assertThat(AuthzUtils.authorizeRoles(roles, "hasRole('ROLE_A') && !hasRole('ROLE_B')"), equalTo(false));
	}

	@Test
	public void testAuthorizeRoles() throws Exception {
		List<String> roles = Arrays.asList("A,B,C".split(","));
		List<String> ifAllGranted = Arrays.asList("A,B,C".split(","));
		List<String> ifAnyGranted = Arrays.asList("C,D".split(","));
		List<String> ifNotGranted = Arrays.asList("D".split(","));
		assertThat(AuthzUtils.authorizeRoles(roles, String.join(",", ifAllGranted), null, null), equalTo(true));
		assertThat(AuthzUtils.authorizeRoles(roles, null, String.join(",", ifAnyGranted), null), equalTo(true));
		assertThat(AuthzUtils.authorizeRoles(roles, null, null, String.join(",", ifNotGranted)), equalTo(true));
		assertThat(AuthzUtils.authorizeRoles(roles, ifAllGranted.toArray(new String[ifAllGranted.size()]), null, null),
				equalTo(true));
		assertThat(AuthzUtils.authorizeRoles(roles, null, ifAnyGranted.toArray(new String[0]), null), equalTo(true));
		assertThat(AuthzUtils.authorizeRoles(roles, null, null, ifNotGranted.toArray(new String[0])), equalTo(true));
		ifAllGranted = Arrays.asList("A,B,C,D".split(","));
		ifAnyGranted = Arrays.asList("D,E".split(","));
		ifNotGranted = Arrays.asList("C,D".split(","));
		assertThat(AuthzUtils.authorizeRoles(roles, String.join(",", ifAllGranted), null, null), equalTo(false));
		assertThat(AuthzUtils.authorizeRoles(roles, null, String.join(",", ifAnyGranted), null), equalTo(false));
		assertThat(AuthzUtils.authorizeRoles(roles, null, null, String.join(",", ifNotGranted)), equalTo(false));
		assertThat(AuthzUtils.authorizeRoles(roles, ifAllGranted.toArray(new String[ifAllGranted.size()]), null, null),
				equalTo(false));
		assertThat(AuthzUtils.authorizeRoles(roles, null, ifAnyGranted.toArray(new String[ifAnyGranted.size()]), null),
				equalTo(false));
		assertThat(AuthzUtils.authorizeRoles(roles, null, null, ifNotGranted.toArray(new String[ifNotGranted.size()])),
				equalTo(false));
		ifAllGranted = Arrays.asList("A,B C".split("\\s"));
		ifAnyGranted = Arrays.asList("C,D".split("\\s"));
		ifNotGranted = Arrays.asList("D,E".split("\\s"));
		assertThat(AuthzUtils.authorizeRoles(roles, ifAllGranted.toArray(new String[ifAllGranted.size()]), null, null),
				equalTo(true));
		assertThat(AuthzUtils.authorizeRoles(roles, null, ifAnyGranted.toArray(new String[ifAnyGranted.size()]), null),
				equalTo(true));
		assertThat(AuthzUtils.authorizeRoles(roles, null, null, ifNotGranted.toArray(new String[ifNotGranted.size()])),
				equalTo(true));
		ifAllGranted = Arrays.asList("A,B C,D".split("\\s"));
		ifAnyGranted = Arrays.asList("D,E".split("\\s"));
		ifNotGranted = Arrays.asList("C,D".split("\\s"));
		assertThat(AuthzUtils.authorizeRoles(roles, ifAllGranted.toArray(new String[ifAllGranted.size()]), null, null),
				equalTo(false));
		assertThat(AuthzUtils.authorizeRoles(roles, null, ifAnyGranted.toArray(new String[ifAnyGranted.size()]), null),
				equalTo(false));
		assertThat(AuthzUtils.authorizeRoles(roles, null, null, ifNotGranted.toArray(new String[ifNotGranted.size()])),
				equalTo(false));
	}

}
