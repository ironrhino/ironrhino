package org.ironrhino.core.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

public class AuthzUtilsTest {

	@Test
	public void testAuthorizeRoles() throws Exception {
		List<String> roles = Arrays.asList("A,B,C".split(","));
		List<String> ifAllGranted = Arrays.asList("A,B,C".split(","));
		List<String> ifAnyGranted = Arrays.asList("C,D".split(","));
		List<String> ifNotGranted = Arrays.asList("D".split(","));
		assertTrue(AuthzUtils.authorizeRoles(roles, StringUtils.join(ifAllGranted, ","), null, null));
		assertTrue(AuthzUtils.authorizeRoles(roles, null, StringUtils.join(ifAnyGranted, ","), null));
		assertTrue(AuthzUtils.authorizeRoles(roles, null, null, StringUtils.join(ifNotGranted, ",")));
		assertTrue(AuthzUtils.authorizeRoles(roles, ifAllGranted.toArray(new String[0]), null, null));
		assertTrue(AuthzUtils.authorizeRoles(roles, null, ifAnyGranted.toArray(new String[0]), null));
		assertTrue(AuthzUtils.authorizeRoles(roles, null, null, ifNotGranted.toArray(new String[0])));
		ifAllGranted = Arrays.asList("A,B,C,D".split(","));
		ifAnyGranted = Arrays.asList("D,E".split(","));
		ifNotGranted = Arrays.asList("C,D".split(","));
		assertFalse(AuthzUtils.authorizeRoles(roles, StringUtils.join(ifAllGranted, ","), null, null));
		assertFalse(AuthzUtils.authorizeRoles(roles, null, StringUtils.join(ifAnyGranted, ","), null));
		assertFalse(AuthzUtils.authorizeRoles(roles, null, null, StringUtils.join(ifNotGranted, ",")));
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
