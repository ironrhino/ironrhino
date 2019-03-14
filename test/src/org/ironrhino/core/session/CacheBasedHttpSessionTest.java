package org.ironrhino.core.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.Cookie;

import org.ironrhino.core.cache.CacheManager;
import org.ironrhino.core.session.impl.CacheBasedHttpSessionStore;
import org.ironrhino.core.util.CodecUtils;
import org.ironrhino.core.util.JsonUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "httpSessionManager.alwaysUseCacheBased=true")
public class CacheBasedHttpSessionTest extends BaseHttpSessionTest {

	private final static String CACHE_NAMESPACE = CacheBasedHttpSessionStore.CACHE_NAMESPACE;

	@Autowired
	protected HttpSessionStore cacheBased;

	@Autowired
	protected CacheManager cacheManager;

	@Autowired
	protected UserDetailsService userDetailsService;

	@Before
	public void clearInvocations() {
		Mockito.clearInvocations(httpSessionManager, cacheBased, request, response);
	}

	@Test
	public void testWithoutSessionTracker() {
		// initialize httpSession
		request.setCookies(new Cookie[0]);
		WrappedHttpSession session = new WrappedHttpSession(request, response, servletContext, httpSessionManager);
		assertTrue(session.isNew());
		assertTrue(session.isCacheBased());
		assertEquals(session.getId(), session.getSessionTracker());
		assertEquals(session.getNow(), session.getCreationTime());
		assertEquals(session.getNow(), session.getLastAccessedTime());
		then(httpSessionManager).should().initialize(session);
		then(cacheBased).should().initialize(session);

		// save httpSession
		String sessionTracker = session.getSessionTracker();
		httpSessionManager.save(session);
		assertEquals(sessionTracker, session.getSessionTracker());
		then(response).should().addCookie(argThat(c -> sessionTrackerName.equals(c.getName()) && 0 != c.getMaxAge()));
		then(cacheBased).should().save(session);
	}

	@Test
	public void testWithCachedSessionCookie() {
		UserDetails ud = mock(UserDetails.class);
		given(ud.getUsername()).willReturn("test");
		given(ud.getPassword()).willReturn("password");
		given(ud.isEnabled()).willReturn(true);
		given(ud.isAccountNonExpired()).willReturn(true);
		given(ud.isAccountNonLocked()).willReturn(true);
		given(ud.isCredentialsNonExpired()).willReturn(true);
		given(userDetailsService.loadUserByUsername("test")).willReturn(ud);

		Map<String, String> compressedMap = Collections.singletonMap(SPRING_SECURITY_CONTEXT_KEY,
				CodecUtils.md5Hex("password") + ",test");
		String sessionTracker = JsonUtils.toJson(compressedMap);
		String sessionId = CodecUtils.nextId();
		given(cacheManager.getWithTti(sessionId, CACHE_NAMESPACE, maxInactiveInterval, TimeUnit.SECONDS))
				.willReturn(sessionTracker);
		request.setCookies(new Cookie(sessionTrackerName, sessionId));

		// initialize httpSession
		WrappedHttpSession session = new WrappedHttpSession(request, response, servletContext, httpSessionManager);
		SecurityContext sc = (SecurityContext) session.getAttribute(SPRING_SECURITY_CONTEXT_KEY);
		assertFalse(session.isNew());
		assertTrue(session.isCacheBased());
		assertEquals(session.getNow(), session.getCreationTime());
		assertEquals(session.getNow(), session.getLastAccessedTime());
		assertTrue(sc.getAuthentication().isAuthenticated());
		assertEquals("test", sc.getAuthentication().getName());
		then(httpSessionManager).should().initialize(session);
		then(cacheBased).should().initialize(session);

		// save httpSession
		httpSessionManager.save(session);
		assertEquals(sessionId, session.getSessionTracker());
		then(response).should(never()).addCookie(any(Cookie.class));
	}
}
