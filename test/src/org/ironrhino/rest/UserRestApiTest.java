package org.ironrhino.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;

import org.ironrhino.core.model.ResultPage;
import org.ironrhino.core.security.role.UserRole;
import org.ironrhino.core.servlet.MainAppInitializer;
import org.ironrhino.core.spring.http.client.RestTemplate;
import org.ironrhino.core.util.JsonUtils;
import org.ironrhino.rest.client.RestApiFactoryBean;
import org.ironrhino.rest.client.UserClient;
import org.ironrhino.security.domain.User;
import org.ironrhino.security.service.UserManager;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.client.MockMvcClientHttpRequestFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.JsonNode;

@RunWith(SpringRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = UserRestApiConfig.class)
public class UserRestApiTest {

	@Autowired
	private WebApplicationContext wac;
	@Autowired
	private UserManager userManager;
	@Autowired
	private UserDetailsService userDetailsService;
	@Autowired
	private PasswordEncoder passwordEncoder;

	private MockMvc mockMvc;

	private UserClient userClient;

	private org.ironrhino.security.model.User admin;

	private org.ironrhino.security.model.User builtin;

	private org.ironrhino.security.model.User disabled;

	protected org.ironrhino.security.model.User createUser(String name, String password, boolean enabled,
			String... roles) {
		org.ironrhino.security.model.User user = new org.ironrhino.security.model.User();
		user.setUsername(name);
		user.setName(name);
		user.setPassword(passwordEncoder.encode(password));
		user.setEnabled(enabled);
		user.setAuthorities(AuthorityUtils.createAuthorityList(roles));
		return user;
	}

	@PostConstruct
	public void afterPropertiesSet() throws Exception {
		mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
		MainAppInitializer.SERVLET_CONTEXT = mockMvc.getDispatcherServlet().getServletContext();

		RestTemplate restTemplate = new RestTemplate(new MockMvcClientHttpRequestFactory(mockMvc));
		userClient = RestApiFactoryBean.create(UserClient.class, restTemplate);

		admin = createUser("admin", "admin", true, UserRole.ROLE_ADMINISTRATOR);
		builtin = createUser("builtin", "builtin", true, UserRole.ROLE_BUILTIN_USER);
		disabled = createUser("disabled", "disabled", false);
		ResultPage<org.ironrhino.security.model.User> rp = new ResultPage<>();
		rp.setPageNo(1);
		rp.setPageSize(1);
		rp.setTotalResults(1);
		rp.setResult(Arrays.asList(admin));

		when(userDetailsService.loadUserByUsername("admin")).thenReturn(admin);
		when(userDetailsService.loadUserByUsername("builtin")).thenReturn(builtin);
		when(userManager.loadUserByUsername("admin")).thenReturn(admin);
		when(userManager.loadUserByUsername("builtin")).thenReturn(builtin);
		when(userManager.loadUserByUsername("disabled")).thenReturn(disabled);
		when(userManager.findAll()).thenReturn(Arrays.asList(admin, builtin));
		when(userManager.findByResultPage(any())).thenReturn(rp);
	}

	@After
	public void clearInvocation() {
		clearInvocations(userDetailsService, userManager);
	}

	@Test
	public void testGetSelf() {
		Authentication auth = new UsernamePasswordAuthenticationToken(builtin, builtin.getPassword(),
				builtin.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(auth);
		User self = userClient.self();
		assertEquals("builtin", self.getUsername());
		SecurityContextHolder.getContext().setAuthentication(null);
	}

	@Test
	@WithUserDetails("builtin")
	public void testGetSelfWithUserDetails() {
		User self = userClient.self();
		assertEquals("builtin", self.getUsername());
		verify(userDetailsService).loadUserByUsername(eq("builtin"));
	}

	@Test
	public void testGetSelfWithNoUser() {
		RestStatus rs = null;
		try {
			userClient.self();
		} catch (RestStatus restStatus) {
			rs = restStatus;
		}
		assertNotNull(rs);
		assertEquals(RestStatus.CODE_NOT_FOUND, rs.getCode());
		assertTrue(rs.getCause() instanceof HttpClientErrorException);
	}

	@Test
	@WithUserDetails("admin")
	public void testGetSelfWithUnauthorizedUser() {
		RestStatus rs = null;
		try {
			userClient.self();
		} catch (RestStatus restStatus) {
			rs = restStatus;
		}
		assertNotNull(rs);
		assertEquals(RestStatus.CODE_UNAUTHORIZED, rs.getCode());
		assertTrue(rs.getCause() instanceof HttpClientErrorException);
		verify(userDetailsService).loadUserByUsername("admin");
	}

	@Test
	@WithUserDetails("admin")
	public void testGetAll() {
		List<User> userList = userClient.all();
		assertNotNull(userList);
		assertEquals(2, userList.size());
	}

	@Test
	@WithUserDetails("admin")
	public void testPaged() {
		ResultPage<User> resultPage = userClient.paged(1, 1);
		assertEquals(1, resultPage.getPageNo());
		assertEquals(1, resultPage.getPageSize());
		assertEquals(1, resultPage.getResult().size());
	}

	@Test
	@WithUserDetails("admin")
	public void testRequestParamDefaultValue() {
		userClient.paged(null, null);
		verify(userManager).findByResultPage(argThat(rp -> rp.getPageNo() == 1 && rp.getPageSize() == 10));
	}

	@Test(expected = IllegalArgumentException.class)
	@WithUserDetails("admin")
	public void testPagedResultWithValidator() {
		userClient.pagedResultWithValidator(1, 1);
	}

	@Test
	@WithUserDetails("builtin")
	public void testPatch() {
		User user = new User();
		user.setUsername("builtin");
		user.setPassword("123456");
		userClient.patch(user);
		verify(userManager).save(argThat(u -> passwordEncoder.matches("123456", u.getPassword())));
	}

	@Test
	@WithUserDetails("builtin")
	public void testValidatePassword() {
		User user = new User();
		user.setUsername("builtin");
		user.setPassword("builtin");
		RestStatus rs = userClient.validatePassword(user);
		assertEquals(RestStatus.CODE_OK, rs.getCode());

		user.setPassword("123456");
		rs = userClient.validatePassword(user);
		assertEquals(RestStatus.CODE_FIELD_INVALID, rs.getCode());
	}

	@Test
	@WithUserDetails("admin")
	public void testDelete() {
		RestStatus rs = null;
		try {
			userClient.delete("admin");
		} catch (RestStatus e) {
			rs = e;
		}
		assertNotNull(rs);
		assertEquals(RestStatus.CODE_FORBIDDEN, rs.getCode());

		userClient.delete("disabled");
		verify(userManager)
				.delete(argThat((org.ironrhino.security.model.User u) -> u.getUsername().equals("disabled")));
	}

	@Test
	@WithUserDetails("builtin")
	public void testGetStream() throws IOException {
		InputStream in = userClient.getStream();
		JsonNode jsonNode = JsonUtils.createNewObjectMapper().readTree(in);
		assertEquals("builtin", jsonNode.get("username").asText());
		assertEquals("builtin", jsonNode.get("name").asText());
	}

	@Test
	@WithUserDetails("builtin")
	public void testPostStream() {
		RestStatus rs = null;
		try {
			InputStream is = new ByteArrayInputStream("test".getBytes());
			userClient.postStream(is);
		} catch (RestStatus e) {
			rs = e;
		}
		assertEquals(rs.getCode(), RestStatus.CODE_INTERNAL_SERVER_ERROR);
		assertTrue(rs.getCause() instanceof HttpServerErrorException);
	}
}
