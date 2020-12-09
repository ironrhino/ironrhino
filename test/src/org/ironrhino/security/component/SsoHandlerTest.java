package org.ironrhino.security.component;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ironrhino.security.domain.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = SsoHandlerTest.Config.class)
@TestPropertySource(properties = "portal.baseUrl=http://portal.cywb.com")
public class SsoHandlerTest {

	@Autowired
	private SsoHandler ssoHandler;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private UserDetailsService userDetailsService;

	@Test
	public void testStrictAccess() throws IOException {
		HttpServletRequest request = mock(HttpServletRequest.class);
		given(request.getRequestURL()).willReturn(new StringBuffer("http://127.0.0.0.1:8080"));
		assertThat(ssoHandler.handle(request, mock(HttpServletResponse.class)), is(ssoHandler.strictAccess));
		given(request.getRequestURL()).willReturn(new StringBuffer("http://www.baidu.com"));
		assertThat(ssoHandler.handle(request, mock(HttpServletResponse.class)), is(ssoHandler.strictAccess));
	}

	@Test
	public void testAuthenticated() throws IOException {
		Authentication authentication = mock(Authentication.class);
		SecurityContext context = mock(SecurityContext.class);
		given(authentication.isAuthenticated()).willReturn(true);
		given(context.getAuthentication()).willReturn(authentication);
		SecurityContextHolder.setContext(context);

		HttpServletRequest request = mock(HttpServletRequest.class);
		given(request.getRequestURL()).willReturn(new StringBuffer("http://app.cywb.com"));
		assertThat(ssoHandler.handle(request, mock(HttpServletResponse.class)), is(false));
		SecurityContextHolder.clearContext();
	}

	@Test
	public void testRedirect() throws IOException {
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		given(request.getRequestURL()).willReturn(new StringBuffer("http://app.cywb.com"));
		given(request.getQueryString()).willReturn("v=123456");
		assertThat(ssoHandler.handle(request, response), is(true));
		then(response).should().sendRedirect(eq("http://portal.cywb.com/login?targetUrl="
				+ URLEncoder.encode("http://app.cywb.com?v=123456", "UTF-8")));
	}

	@Test
	public void testNotFound() throws URISyntaxException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		HttpServletResponse response = mock(HttpServletResponse.class);
		request.setScheme("http");
		request.setServerName("app.cywb.com");
		request.setCookies(new Cookie("T", "1234567890"));

		URI apiUri = new URI("http://portal.cywb.com/api/user/@self");
		willThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "", HttpHeaders.EMPTY, new byte[0],
				StandardCharsets.UTF_8)).given(restTemplate).exchange(argThat(entity -> entity.getUrl().equals(apiUri)),
						eq(SsoHandler.User.class));
		assertThat(ssoHandler.handle(request, response), is(true));
		then(response).should().sendRedirect(
				eq("http://portal.cywb.com/login?targetUrl=" + URLEncoder.encode("http://app.cywb.com", "UTF-8")));
	}

	@Test
	public void testMapUserFormApi() throws URISyntaxException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		HttpServletResponse response = mock(HttpServletResponse.class);
		request.setScheme("http");
		request.setServerName("app.cywb.com");
		request.setCookies(new Cookie("T", "1234567890"));

		SsoHandler.User userFromApi = new SsoHandler.User();
		userFromApi.setUsername("admin");
		userFromApi.setRoles(new HashSet<>(Arrays.asList("ROLE_PORTAL_1", "ROLE_PORTAL_2")));
		URI apiUri = new URI("http://portal.cywb.com/api/user/@self");
		willReturn(new ResponseEntity<SsoHandler.User>(userFromApi, HttpStatus.OK)).given(restTemplate)
				.exchange(argThat(entity -> entity.getUrl().equals(apiUri)), eq(SsoHandler.User.class));

		User user = mock(User.class);
		given(user.getUsername()).willReturn("admin");
		given(user.getPassword()).willReturn("password");
		given(user.getAuthorities()).willReturn(AuthorityUtils.createAuthorityList("ROLE_APP_1", "ROLE_APP_2"));
		given(userDetailsService.loadUserByUsername("admin")).willReturn(user);

		assertThat(ssoHandler.handle(request, response), is(false));
		SecurityContext sc = SecurityContextHolder.getContext();
		Authentication authentication = sc.getAuthentication();
		assertThat(authentication, is(notNullValue()));
		assertThat(authentication.getName(), is("admin"));
		assertThat(authentication.getAuthorities(), is(notNullValue()));
		assertThat(authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
				.collect(Collectors.toList())
				.containsAll(Arrays.asList("ROLE_PORTAL_1", "ROLE_PORTAL_2", "ROLE_APP_1", "ROLE_APP_2")), is(true));
		SecurityContextHolder.clearContext();
	}

	@Test
	public void testUnsupportedOperation() {
		SsoHandler.User userFromApi = new SsoHandler.User();
		userFromApi.setUsername("admin");
		userFromApi.setRoles(new HashSet<>(Arrays.asList("ROLE_PORTAL_1")));

		User user = mock(User.class);
		given(user.getUsername()).willReturn("admin");
		given(user.getPassword()).willReturn("password");
		given(user.getAuthorities()).willReturn(Arrays.asList(new SimpleGrantedAuthority("ROLE_APP_1")));
		given(userDetailsService.loadUserByUsername("admin")).willReturn(user);

		UserDetails userDetails = ssoHandler.map(userFromApi);
		assertThat(userDetails.getAuthorities(), is(notNullValue()));
		Collection<String> roles = userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority)
				.collect(Collectors.toList());
		assertThat(roles.contains("ROLE_PORTAL_1"), is(false));
		assertThat(roles.contains("ROLE_APP_1"), is(true));
	}

	static class Config {

		@Bean
		public SsoHandler ssoHandler() {
			return new SsoHandler();
		}

		@Bean
		public UserDetailsService userDetailsService() {
			return mock(UserDetailsService.class);
		}

		@Bean
		public RestTemplate restTemplate() {
			return mock(RestTemplate.class);
		}
	}
}
