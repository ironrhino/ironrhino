package org.ironrhino.core.session;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import javax.annotation.PostConstruct;

import org.ironrhino.core.cache.CacheManager;
import org.ironrhino.core.cache.impl.Cache2kCacheManager;
import org.ironrhino.core.session.BaseHttpSessionTest.HttpSessionConfiguration;
import org.ironrhino.core.session.impl.CacheBasedHttpSessionStore;
import org.ironrhino.core.session.impl.CookieBasedHttpSessionStore;
import org.ironrhino.core.session.impl.DefaultHttpSessionManager;
import org.ironrhino.core.session.impl.DefaultSessionCompressor;
import org.ironrhino.core.session.impl.SecurityContextSessionCompressor;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = HttpSessionConfiguration.class)
public abstract class BaseHttpSessionTest {

	protected static final String SPRING_SECURITY_CONTEXT_KEY = HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

	protected final MockServletContext servletContext = new MockServletContext();

	protected final MockHttpServletRequest request = spy(new MockHttpServletRequest(servletContext));

	protected final MockHttpServletResponse response = spy(new MockHttpServletResponse());

	@Autowired
	protected DefaultHttpSessionManager httpSessionManager;

	@Value("${httpSessionManager.alwaysUseCacheBased:}")
	protected Boolean alwaysUseCacheBased;

	protected String sessionTrackerName;

	protected int maxInactiveInterval;

	protected int minActiveInterval;

	@PostConstruct
	public void afterPropertiesSet() {
		this.sessionTrackerName = httpSessionManager.getSessionTrackerName();
		this.maxInactiveInterval = httpSessionManager.getMaxInactiveInterval();
		this.minActiveInterval = httpSessionManager.getMinActiveInterval();
	}

	@Configuration
	static class HttpSessionConfiguration {

		@Autowired
		private CacheManager cacheManager;

		@Bean
		public static CacheManager cacheManager() {
			return spy(new Cache2kCacheManager());
		}

		@Bean
		public HttpSessionManager httpSessionManager() {
			return spy(new DefaultHttpSessionManager());
		}

		@Bean
		public HttpSessionStore cookieBased() {
			return spy(new CookieBasedHttpSessionStore());
		}

		@Bean
		public HttpSessionStore cacheBased() {
			return spy(new CacheBasedHttpSessionStore(cacheManager));
		}

		@Bean
		public SessionCompressorManager sessionCompressorManager() {
			return spy(new SessionCompressorManager());
		}

		@Bean
		public SessionCompressor<Object> sessionCompressor() {
			return new DefaultSessionCompressor();
		}

		@Bean
		public SessionCompressor<SecurityContext> securityContextSessionCompressor() {
			return spy(new SecurityContextSessionCompressor());
		}

		@Bean
		public UserDetailsService mockUserDetailsService() {
			return mock(UserDetailsService.class);
		}
	}
}
