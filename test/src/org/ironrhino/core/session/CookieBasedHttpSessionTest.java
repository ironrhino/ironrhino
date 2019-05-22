package org.ironrhino.core.session;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import java.math.BigInteger;
import java.net.URLEncoder;
import java.util.Collections;

import javax.annotation.PostConstruct;
import javax.servlet.http.Cookie;

import org.ironrhino.core.security.util.Blowfish;
import org.ironrhino.core.session.impl.CookieBasedHttpSessionStore;
import org.ironrhino.core.session.impl.DefaultHttpSessionManager;
import org.ironrhino.core.util.CodecUtils;
import org.ironrhino.core.util.JsonUtils;
import org.ironrhino.core.util.NumberUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "httpSessionManager.alwaysUseCacheBased=false")
public class CookieBasedHttpSessionTest extends BaseHttpSessionTest {

	@Value("${httpSessionManager.sessionCookieName:" + HttpSessionManager.DEFAULT_SESSION_COOKIE_NAME + "}")
	protected String sessionCookieName;

	@Autowired
	private DefaultHttpSessionManager httpSessionManager;
	@Autowired
	private CookieBasedHttpSessionStore cookieBased;
	@Autowired
	private SessionCompressorManager sessionCompressorManager;
	@Autowired
	private SessionCompressor<SecurityContext> securityContextSessionCompressor;
	@Autowired
	private UserDetailsService userDetailsService;

	@PostConstruct
	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		givenUserDetails("test", "password", true, true, true, true);
		givenUserDetails("disabled", "password", false, true, true, true);
		givenUserDetails("accountExpired", "password", true, false, true, true);
		givenUserDetails("accountLocked", "password", true, true, false, true);
		givenUserDetails("credentialsExpired", "password", true, true, true, false);
	}

	@Before
	public void clearInvocations() {
		Mockito.clearInvocations(httpSessionManager, cookieBased, sessionCompressorManager,
				securityContextSessionCompressor, userDetailsService, request, response);
	}

	@Test
	public void testWithSessionTracker() {
		String sessionId = CodecUtils.nextId();
		long creationTime = System.currentTimeMillis();
		long lastAccessedTime = creationTime;
		String sessionTracker = createSessionTracker(sessionId, creationTime, lastAccessedTime);
		request.setCookies(new Cookie(sessionTrackerName, sessionTracker));

		// initialize httpSession
		WrappedHttpSession session = new WrappedHttpSession(request, response, servletContext, httpSessionManager);
		assertThat(session.isNew(), is(false));
		assertThat(session.getCreationTime(), is(creationTime));
		assertThat(session.getLastAccessedTime(), is(lastAccessedTime));
		then(httpSessionManager).should().initialize(session);
		then(cookieBased).should().initialize(session);

		// save httpSession
		httpSessionManager.save(session);
		assertThat(session.getSessionTracker(), is(sessionTracker));
		then(response).should(never()).addCookie(any(Cookie.class));
	}

	@Test
	public void testWithoutSessionTracker() {
		request.setCookies(new Cookie[0]);

		// initialize httpSession
		WrappedHttpSession session = new WrappedHttpSession(request, response, servletContext, httpSessionManager);
		assertThat(session.isNew(), is(true));
		assertThat(session.getCreationTime(), is(session.getNow()));
		assertThat(session.getLastAccessedTime(), is(session.getNow()));
		then(httpSessionManager).should().initialize(session);
		then(cookieBased).should().initialize(session);

		// save httpSession
		String sessionTracker = session.getSessionTracker();
		httpSessionManager.save(session);
		assertThat(session.getSessionTracker(), is(sessionTracker));
		then(response).should().addCookie(argThat(this::isValidSessionTracker));
	}

	@Test
	public void testWithUnseparatedSessionTracker() {
		String unseparatedSessionTracker = "0123456789";
		request.setCookies(new Cookie(httpSessionManager.getSessionTrackerName(), unseparatedSessionTracker));

		// initialize httpSession
		WrappedHttpSession session = new WrappedHttpSession(request, response, servletContext, httpSessionManager);
		assertThat(session.isNew(), is(true));
		assertThat(session.getCreationTime(), is(session.getNow()));
		assertThat(session.getLastAccessedTime(), is(session.getNow()));
		assertThat(session.getSessionTracker(), is(unseparatedSessionTracker));
		then(httpSessionManager).should().initialize(session);
		then(cookieBased).should().initialize(session);

		// save httpSession
		httpSessionManager.save(session);
		assertThat(session.getSessionTracker(), is(not(unseparatedSessionTracker)));
		then(response).should().addCookie(argThat(this::isValidSessionTracker));
	}

	@Test
	public void testWithInvalidSessionCookie() {
		String sessionId = CodecUtils.nextId();
		long creationTime = System.currentTimeMillis();
		long lastAccessedTime = creationTime;
		String sessionTracker = createSessionTracker(sessionId, creationTime, lastAccessedTime);
		request.setCookies(new Cookie(sessionTrackerName, sessionTracker), new Cookie(sessionCookieName, "0123456789"));

		// initialize httpSession
		WrappedHttpSession session = new WrappedHttpSession(request, response, servletContext, httpSessionManager);
		assertThat(session.isNew(), is(false));
		assertThat(session.getCreationTime(), is(creationTime));
		assertThat(session.getLastAccessedTime(), is(lastAccessedTime));
		assertThat(session.getSessionTracker(), is(sessionTracker));
		then(httpSessionManager).should().initialize(session);
		then(cookieBased).should().initialize(session);
		then(cookieBased).should().invalidate(session);
		then(response).should().addCookie(argThat(this::isInvalidSessionCookie));
	}

	@Test
	public void testWithSessionCookie() throws Exception {
		String sessionId = CodecUtils.nextId();
		long creationTime = System.currentTimeMillis();
		long lastAccessedTime = creationTime;
		String sessionTracker = createSessionTracker(sessionId, creationTime, lastAccessedTime);
		String sessionCookie = createSessionCookie(sessionId, creationTime, "test", "password");
		request.setCookies(new Cookie(sessionTrackerName, sessionTracker),
				new Cookie(sessionCookieName, URLEncoder.encode(sessionCookie, "UTF-8")));

		// initialize httpSession
		WrappedHttpSession session = new WrappedHttpSession(request, response, servletContext, httpSessionManager);
		SecurityContext sc = (SecurityContext) session.getAttribute(SPRING_SECURITY_CONTEXT_KEY);
		assertThat(session.isNew(), is(false));
		assertThat(session.getCreationTime(), is(creationTime));
		assertThat(session.getLastAccessedTime(), is(lastAccessedTime));
		assertThat(session.getSessionTracker(), is(sessionTracker));
		assertThat(sc.getAuthentication().isAuthenticated(), is(true));
		assertThat(sc.getAuthentication().getName(), is("test"));
		then(httpSessionManager).should().initialize(session);
		then(cookieBased).should().initialize(session);
		then(securityContextSessionCompressor).should().uncompress(eq(CodecUtils.md5Hex("password") + ",test"));

		// save httpSession
		httpSessionManager.save(session);
		then(response).should(never()).addCookie(any(Cookie.class));
		then(sessionCompressorManager).should(never()).compress(session);
	}

	@Test
	public void testWithLargeSessionCookie() throws Exception {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < 2560; i++) {
			builder.append("test");
		}
		String username = builder.toString();
		String password = "password";
		UserDetails ud = mock(UserDetails.class);
		given(ud.getUsername()).willReturn(username);
		given(ud.getPassword()).willReturn(password);
		given(ud.isEnabled()).willReturn(true);
		given(ud.isAccountNonExpired()).willReturn(true);
		given(ud.isAccountNonLocked()).willReturn(true);
		given(ud.isCredentialsNonExpired()).willReturn(true);
		given(userDetailsService.loadUserByUsername(username)).willReturn(ud);

		String sessionId = CodecUtils.nextId();
		long creationTime = System.currentTimeMillis();
		long lastAccessedTime = creationTime;
		String sessionTracker = createSessionTracker(sessionId, creationTime, lastAccessedTime);
		String sessionCookie = createSessionCookie(sessionId, creationTime, username, password);
		int pieces = sessionCookie.length() / CookieBasedHttpSessionStore.SINGLE_COOKIE_SIZE;
		if (sessionCookie.length() % CookieBasedHttpSessionStore.SINGLE_COOKIE_SIZE != 0)
			pieces++;
		Cookie[] sessionCookies = new Cookie[pieces + 1];
		for (int i = 0; i < pieces; i++) {
			sessionCookies[i] = new Cookie(i == 0 ? sessionCookieName : sessionCookieName + (i - 1),
					URLEncoder.encode(
							sessionCookie
									.substring(i * CookieBasedHttpSessionStore.SINGLE_COOKIE_SIZE,
											i == pieces - 1 ? sessionCookie.length()
													: (i + 1) * CookieBasedHttpSessionStore.SINGLE_COOKIE_SIZE),
							"UTF-8"));
		}
		sessionCookies[pieces] = new Cookie(sessionTrackerName, sessionTracker);
		request.setCookies(sessionCookies);

		// initialize httpSession
		WrappedHttpSession session = new WrappedHttpSession(request, response, servletContext, httpSessionManager);
		SecurityContext sc = (SecurityContext) session.getAttribute(SPRING_SECURITY_CONTEXT_KEY);
		assertThat(sc.getAuthentication().isAuthenticated(), is(true));
		assertThat(sc.getAuthentication().getName().length(), is(10240));
	}

	@Test
	public void testWithExpiredSessionTracker() throws Exception {
		String sessionId = CodecUtils.nextId();
		long creationTime = System.currentTimeMillis() - httpSessionManager.getMinActiveInterval() * 1000 - 1000;
		long lastAccessedTime = creationTime;
		String expiredSessionTracker = createSessionTracker(sessionId, creationTime, lastAccessedTime);
		String sessionCookie = createSessionCookie(sessionId, creationTime, "test", "password");
		request.setCookies(new Cookie(sessionTrackerName, expiredSessionTracker),
				new Cookie(sessionCookieName, URLEncoder.encode(sessionCookie, "UTF-8")));

		// initialize httpSession
		WrappedHttpSession session = new WrappedHttpSession(request, response, servletContext, httpSessionManager);
		assertThat(session.getCreationTime(), is(creationTime));
		assertThat(session.getLastAccessedTime(), is(lastAccessedTime));
		assertThat(session.getSessionTracker(), is(expiredSessionTracker));
		then(httpSessionManager).should().initialize(session);
		then(cookieBased).should().initialize(session);
		then(response).should(never()).addCookie(any(Cookie.class));

		// save httpSession
		String sessionTracker = session.getSessionTracker();
		httpSessionManager.save(session);
		assertThat(session.getCreationTime(), is(creationTime));
		assertThat(session.getLastAccessedTime(), is(not(lastAccessedTime)));
		assertThat(session.getSessionTracker(), is(not(sessionTracker)));
		then(response).should().addCookie(argThat(this::isValidSessionTracker));
		then(sessionCompressorManager).should(never()).compress(session);
	}

	@Test
	public void testWithExpiredSessionCookie() throws Exception {
		String sessionId = CodecUtils.nextId();
		long creationTime = System.currentTimeMillis() - httpSessionManager.getMaxInactiveInterval() * 1000 - 1000;
		long lastAccessedTime = creationTime;
		String expiredSessionTracker = createSessionTracker(sessionId, creationTime, lastAccessedTime);
		String sessionCookie = createSessionCookie(sessionId, creationTime, "user", "password");
		request.setCookies(new Cookie(sessionTrackerName, expiredSessionTracker),
				new Cookie(sessionCookieName, URLEncoder.encode(sessionCookie, "UTF-8")));

		// initialize httpSession
		WrappedHttpSession session = new WrappedHttpSession(request, response, servletContext, httpSessionManager);
		assertThat(session.isNew(), is(false));
		assertThat(session.isInvalid(), is(true));
		assertThat(session.getCreationTime(), is(not(creationTime)));
		assertThat(session.getLastAccessedTime(), is(not(lastAccessedTime)));
		assertThat(session.getSessionTracker(), is(not(expiredSessionTracker)));
		then(httpSessionManager).should().invalidate(session);
		then(cookieBased).should().invalidate(session);
		then(response).should().addCookie(argThat(this::isInvalidSessionCookie));

		// save httpSession
		String sessionTracker = session.getSessionTracker();
		httpSessionManager.save(session);
		assertThat(session.getSessionTracker(), is(not(sessionTracker)));
		then(response).should().addCookie(argThat(this::isValidSessionTracker));
		then(sessionCompressorManager).should(never()).compress(session);
	}

	@Test
	public void testWithSessionMap() throws Exception {
		UserDetails ud = userDetailsService.loadUserByUsername("test");
		Authentication auth = new UsernamePasswordAuthenticationToken(ud, ud.getPassword(), ud.getAuthorities());
		SecurityContext sc = new SecurityContextImpl(auth);
		request.setAttribute(HttpSessionManager.REQUEST_ATTRIBUTE_KEY_SESSION_MAP_FOR_API,
				Collections.singletonMap(SPRING_SECURITY_CONTEXT_KEY, sc));

		// initialize httpSession
		WrappedHttpSession session = new WrappedHttpSession(request, response, servletContext, httpSessionManager);
		sc = (SecurityContext) session.getAttribute(SPRING_SECURITY_CONTEXT_KEY);
		assertThat(session.isNew(), is(false));
		assertThat(session.getCreationTime(), is(0L));
		assertThat(session.getLastAccessedTime(), is(0L));
		assertThat(sc.getAuthentication().getName(), is("test"));
		then(httpSessionManager).should().initialize(session);
		then(cookieBased).should(never()).initialize(session);

		// save httpSession
		httpSessionManager.save(session);
		assertThat(request.getAttribute(HttpSessionManager.REQUEST_ATTRIBUTE_KEY_SESSION_MAP_FOR_API), is(nullValue()));
		request.clearAttributes();
	}

	@Test(expected = DisabledException.class)
	public void testWithDisabledUserDetails() throws Exception {
		String sessionId = CodecUtils.nextId();
		long creationTime = System.currentTimeMillis();
		String sessionTracker = createSessionTracker(sessionId, creationTime, creationTime);
		String sessionCookie = createSessionCookie(sessionId, creationTime, "disabled", "password");
		request.setCookies(new Cookie(sessionTrackerName, sessionTracker),
				new Cookie(sessionCookieName, URLEncoder.encode(sessionCookie, "UTF-8")));
		new WrappedHttpSession(request, response, servletContext, httpSessionManager);
	}

	@Test(expected = AccountExpiredException.class)
	public void testWithAccountExpiredUserDetails() throws Exception {
		String sessionId = CodecUtils.nextId();
		long creationTime = System.currentTimeMillis();
		String sessionTracker = createSessionTracker(sessionId, creationTime, creationTime);
		String sessionCookie = createSessionCookie(sessionId, creationTime, "accountExpired", "password");
		request.setCookies(new Cookie(sessionTrackerName, sessionTracker),
				new Cookie(sessionCookieName, URLEncoder.encode(sessionCookie, "UTF-8")));
		new WrappedHttpSession(request, response, servletContext, httpSessionManager);
	}

	@Test(expected = LockedException.class)
	public void testWithAccountLockedUserDetails() throws Exception {
		String sessionId = CodecUtils.nextId();
		long creationTime = System.currentTimeMillis();
		String sessionTracker = createSessionTracker(sessionId, creationTime, creationTime);
		String sessionCookie = createSessionCookie(sessionId, creationTime, "accountLocked", "password");
		request.setCookies(new Cookie(sessionTrackerName, sessionTracker),
				new Cookie(sessionCookieName, URLEncoder.encode(sessionCookie, "UTF-8")));
		new WrappedHttpSession(request, response, servletContext, httpSessionManager);
	}

	@Test(expected = CredentialsExpiredException.class)
	public void testWithCredentialsExpiredUserDetails() throws Exception {
		String sessionId = CodecUtils.nextId();
		long creationTime = System.currentTimeMillis();
		String sessionTracker = createSessionTracker(sessionId, creationTime, creationTime);
		String sessionCookie = createSessionCookie(sessionId, creationTime, "credentialsExpired", "password");
		request.setCookies(new Cookie(sessionTrackerName, sessionTracker),
				new Cookie(sessionCookieName, URLEncoder.encode(sessionCookie, "UTF-8")));
		new WrappedHttpSession(request, response, servletContext, httpSessionManager);
	}

	protected void givenUserDetails(String username, String password, boolean enabled, boolean accountNonExpired,
			boolean accountNonLocked, boolean credentialsNonExpired) {
		UserDetails ud = mock(UserDetails.class);
		given(ud.getUsername()).willReturn(username);
		given(ud.getPassword()).willReturn(password);
		given(ud.isEnabled()).willReturn(enabled);
		given(ud.isAccountNonExpired()).willReturn(accountNonExpired);
		given(ud.isAccountNonLocked()).willReturn(accountNonLocked);
		given(ud.isCredentialsNonExpired()).willReturn(credentialsNonExpired);
		given(userDetailsService.loadUserByUsername(username)).willReturn(ud);
	}

	protected String createSessionTracker(String sessionId, long creationTime, long lastAccessedTime) {
		return CodecUtils.swap(sessionId + "-" + NumberUtils.decimalToX(62, BigInteger.valueOf(creationTime)) + "-"
				+ NumberUtils.decimalToX(62, BigInteger.valueOf(lastAccessedTime)));
	}

	protected String createSessionCookie(String sessionId, long creationTime, String username, String password) {
		String sessionCookie = NumberUtils.decimalToX(62, BigInteger.valueOf(creationTime)) + JsonUtils.toJson(
				Collections.singletonMap(SPRING_SECURITY_CONTEXT_KEY, CodecUtils.md5Hex(password) + "," + username));
		return Blowfish.encryptWithSalt(sessionCookie, sessionId);
	}

	protected boolean isValidSessionTracker(Cookie c) {
		return c != null && sessionTrackerName.equals(c.getName()) && 0 != c.getMaxAge();
	}

	protected boolean isInvalidSessionTracker(Cookie c) {
		return c != null && sessionTrackerName.equals(c.getName()) && 0 == c.getMaxAge();
	}

	protected boolean isValidSessionCookie(Cookie c) {
		return c != null && sessionCookieName.equals(c.getName()) && 0 != c.getMaxAge();
	}

	protected boolean isInvalidSessionCookie(Cookie c) {
		return c != null && sessionCookieName.equals(c.getName()) && 0 == c.getMaxAge();
	}
}
