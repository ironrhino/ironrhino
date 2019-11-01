package org.ironrhino.rest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.ironrhino.rest.MockMvcResultMatchers.jsonPoint;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;

import org.ironrhino.core.model.ResultPage;
import org.ironrhino.core.security.role.UserRole;
import org.ironrhino.core.spring.ExecutorServiceFactoryBean;
import org.ironrhino.core.spring.http.client.RestTemplate;
import org.ironrhino.core.spring.security.password.MixedPasswordEncoder;
import org.ironrhino.core.util.JsonUtils;
import org.ironrhino.rest.UserRestApiTest.UserRestApiConfiguration;
import org.ironrhino.rest.client.RestApiFactoryBean;
import org.ironrhino.rest.client.RestClientConfiguration.MyJsonValidator;
import org.ironrhino.rest.client.UserClient;
import org.ironrhino.rest.component.AuthorizeAspect;
import org.ironrhino.sample.api.controller.UserController;
import org.ironrhino.security.domain.User;
import org.ironrhino.security.service.UserManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.context.request.async.DeferredResultProcessingInterceptor;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.fasterxml.jackson.databind.JsonNode;

@RunWith(SpringRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = UserRestApiConfiguration.class)
public class UserRestApiTest {

	@Autowired
	private UserManager userManager;
	@Autowired
	private UserDetailsService userDetailsService;
	@Autowired
	private PasswordEncoder passwordEncoder;
	@Autowired
	private DeferredResultProcessingInterceptor deferredResultProcessingInterceptor;
	@Autowired
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

		given(userDetailsService.loadUserByUsername("admin")).willReturn(admin);
		given(userDetailsService.loadUserByUsername("builtin")).willReturn(builtin);
		given(userManager.loadUserByUsername("admin")).willReturn(admin);
		given(userManager.loadUserByUsername("builtin")).willReturn(builtin);
		given(userManager.loadUserByUsername("disabled")).willReturn(disabled);
		given(userManager.findAll()).willReturn(Arrays.asList(admin, builtin));
		given(userManager.findByResultPage(any())).willReturn(rp);
		clearInvocations(userDetailsService, userManager, deferredResultProcessingInterceptor);
	}

	@Test
	public void testGetSelf() {
		Authentication auth = new UsernamePasswordAuthenticationToken(builtin, builtin.getPassword(),
				builtin.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(auth);
		User self = userClient.self();
		assertThat(self.getUsername(), is("builtin"));
		SecurityContextHolder.getContext().setAuthentication(null);
	}

	@Test
	@WithUserDetails("builtin")
	public void testGetSelfWithUserDetails() {
		User self = userClient.self();
		assertThat(self.getUsername(), is("builtin"));
		then(userDetailsService).should().loadUserByUsername(eq("builtin"));
	}

	@Test
	public void testGetSelfWithNoUser() {
		RestStatus rs = null;
		try {
			userClient.self();
		} catch (RestStatus restStatus) {
			rs = restStatus;
		}
		assertThat(rs, is(notNullValue()));
		assertThat(rs.getCode(), is(RestStatus.CODE_NOT_FOUND));
		assertThat(rs.getCause() instanceof HttpClientErrorException, is(true));
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
		assertThat(rs, is(notNullValue()));
		assertThat(rs.getCode(), is(RestStatus.CODE_UNAUTHORIZED));
		assertThat(rs.getCause() instanceof HttpClientErrorException, is(true));
		then(userDetailsService).should().loadUserByUsername("admin");
	}

	@Test
	@WithUserDetails("admin")
	public void testGetAll() {
		List<User> userList = userClient.all();
		assertThat(userList, is(notNullValue()));
		assertThat(userList.size(), is(2));
	}

	@Test
	@WithUserDetails("admin")
	public void testPaged() {
		ResultPage<User> resultPage = userClient.paged(1, 1);
		assertThat(resultPage.getPageNo(), is(1));
		assertThat(resultPage.getPageSize(), is(1));
		assertThat(resultPage.getResult().size(), is(1));
		resultPage = userClient.pagedRestResult(1, 1);
		assertThat(resultPage.getPageNo(), is(1));
		assertThat(resultPage.getPageSize(), is(1));
		assertThat(resultPage.getResult().size(), is(1));
		resultPage = userClient.pagedRestResultWithResponseEntity(1, 1).getBody();
		assertThat(resultPage.getPageNo(), is(1));
		assertThat(resultPage.getPageSize(), is(1));
		assertThat(resultPage.getResult().size(), is(1));
	}

	@Test
	@WithUserDetails("admin")
	public void testRequestParamDefaultValue() {
		userClient.paged(null, null);
		then(userManager).should().findByResultPage(argThat(rp -> rp.getPageNo() == 1 && rp.getPageSize() == 10));
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
		then(userManager).should().save(argThat(u -> passwordEncoder.matches("123456", u.getPassword())));
	}

	@Test
	@WithUserDetails("builtin")
	public void testValidatePassword() {
		User user = new User();
		user.setUsername("builtin");
		user.setPassword("builtin");
		RestStatus rs = userClient.validatePassword(user);
		assertThat(rs.getCode(), is(RestStatus.CODE_OK));

		user.setPassword("123456");
		rs = userClient.validatePassword(user);
		assertThat(rs.getCode(), is(RestStatus.CODE_FIELD_INVALID));
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
		assertThat(rs, is(notNullValue()));
		assertThat(rs.getCode(), is(RestStatus.CODE_FORBIDDEN));

		userClient.delete("disabled");
		then(userManager).should()
				.delete(argThat((org.ironrhino.security.model.User u) -> u.getUsername().equals("disabled")));
	}

	@Test
	@WithUserDetails("builtin")
	public void testGetStream() throws IOException {
		InputStream in = userClient.getStream();
		JsonNode jsonNode = JsonUtils.createNewObjectMapper().readTree(in);
		assertThat(jsonNode.get("username").asText(), is("builtin"));
		assertThat(jsonNode.get("name").asText(), is("builtin"));
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
		assertThat(rs, is(notNullValue()));
		assertThat(RestStatus.CODE_INTERNAL_SERVER_ERROR, is(rs.getCode()));
		assertThat(rs.getCause() instanceof HttpServerErrorException, is(true));
	}

	@Test
	@WithUserDetails("admin")
	public void testDeferredResult() throws Exception {
		MvcResult mvcResult = this.mockMvc.perform(get("/user/admin")).andExpect(status().isOk())
				.andExpect(request().asyncStarted()).andExpect(request().asyncResult(admin)).andReturn();

		then(deferredResultProcessingInterceptor).should().beforeConcurrentHandling(any(), any());
		then(deferredResultProcessingInterceptor).should().preProcess(any(), any());
		then(deferredResultProcessingInterceptor).should().postProcess(any(), any(), any());
		then(deferredResultProcessingInterceptor).shouldHaveNoMoreInteractions();

		this.mockMvc.perform(asyncDispatch(mvcResult)).andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPoint("/username").value("admin")).andExpect(jsonPoint("/name").value("admin"))
				.andExpect(jsonPoint("/roles/0").value("ROLE_ADMINISTRATOR")).andReturn();

		then(deferredResultProcessingInterceptor).should().afterCompletion(any(), any());
		then(deferredResultProcessingInterceptor).shouldHaveNoMoreInteractions();
	}

	@Test
	@WithUserDetails("admin")
	public void testDeferredResultNotFound() throws Exception {
		given(userManager.loadUserByUsername("test")).willReturn(null);
		MvcResult mvcResult = this.mockMvc.perform(get("/user/test")).andExpect(status().isOk())
				.andExpect(request().asyncStarted()).andExpect(request().asyncResult(RestStatus.NOT_FOUND)).andReturn();

		then(deferredResultProcessingInterceptor).should().beforeConcurrentHandling(any(), any());
		then(deferredResultProcessingInterceptor).should().preProcess(any(), any());
		then(deferredResultProcessingInterceptor).should().postProcess(any(),
				argThat(result -> RestStatus.NOT_FOUND.equals(result.getResult())), any());
		then(deferredResultProcessingInterceptor).shouldHaveNoMoreInteractions();

		this.mockMvc.perform(asyncDispatch(mvcResult)).andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPoint("/status").value("NOT_FOUND")).andExpect(jsonPoint("/message").value("Not Found"))
				.andReturn();

		then(deferredResultProcessingInterceptor).should().afterCompletion(any(), any());
		then(deferredResultProcessingInterceptor).shouldHaveNoMoreInteractions();
	}

	@Test
	@WithUserDetails("admin")
	public void testFlux() throws Exception {
		MvcResult mvcResult = this.mockMvc.perform(get("/user/flux")).andExpect(status().isOk())
				.andExpect(request().asyncStarted()).andReturn();

		then(deferredResultProcessingInterceptor).should().beforeConcurrentHandling(any(), any());
		then(deferredResultProcessingInterceptor).should().preProcess(any(), any());
		then(deferredResultProcessingInterceptor).should().postProcess(any(), any(), any());
		then(deferredResultProcessingInterceptor).shouldHaveNoMoreInteractions();

		this.mockMvc.perform(asyncDispatch(mvcResult)).andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPoint("/0/username").value("admin")).andExpect(jsonPoint("/0/name").value("admin"))
				.andExpect(jsonPoint("/0/roles/0").value("ROLE_ADMINISTRATOR"));

		then(deferredResultProcessingInterceptor).should().afterCompletion(any(), any());
		then(deferredResultProcessingInterceptor).shouldHaveNoMoreInteractions();
	}

	@Test
	@WithUserDetails("admin")
	public void testMono() throws Exception {
		MvcResult mvcResult = this.mockMvc.perform(get("/user/mono")).andExpect(status().isOk())
				.andExpect(request().asyncStarted()).andReturn();

		then(deferredResultProcessingInterceptor).should().beforeConcurrentHandling(any(), any());
		then(deferredResultProcessingInterceptor).should().preProcess(any(), any());
		then(deferredResultProcessingInterceptor).should().postProcess(any(), any(), any());
		then(deferredResultProcessingInterceptor).shouldHaveNoMoreInteractions();

		this.mockMvc.perform(asyncDispatch(mvcResult)).andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPoint("/username").value("admin")).andExpect(jsonPoint("/name").value("admin"))
				.andExpect(jsonPoint("/roles/0").value("ROLE_ADMINISTRATOR"));

		then(deferredResultProcessingInterceptor).should().afterCompletion(any(), any());
		then(deferredResultProcessingInterceptor).shouldHaveNoMoreInteractions();
	}

	@EnableWebMvc
	@EnableWebSecurity
	@EnableAspectJAutoProxy(proxyTargetClass = true)
	static class UserRestApiConfiguration extends AbstractMockMvcConfigurer {

		@Bean
		public UserDetailsService userDetailsService() {
			return mock(UserDetailsService.class);
		}

		@Bean
		public UserManager userManager() {
			return mock(UserManager.class);
		}

		@Bean
		public ExecutorServiceFactoryBean executorService() {
			return new ExecutorServiceFactoryBean();
		}

		@Bean
		public UserController userController() {
			return new UserController();
		}

		@Bean
		public MixedPasswordEncoder passwordEncoder() {
			return new MixedPasswordEncoder();
		}

		@Bean
		public AuthorizeAspect authorizeAspect() {
			return new AuthorizeAspect();
		}

		@Bean
		public MyJsonValidator myJsonValidator() {
			return new MyJsonValidator();
		}
	}
}
