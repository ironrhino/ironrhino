package org.ironrhino.core.remoting.server;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolationException;

import org.aopalliance.intercept.MethodInvocation;
import org.ironrhino.core.metadata.Scope;
import org.ironrhino.core.remoting.RemotingContext;
import org.ironrhino.core.remoting.ServiceNotFoundException;
import org.ironrhino.core.remoting.ServiceRegistry;
import org.ironrhino.core.remoting.client.HttpInvokerClient;
import org.ironrhino.core.remoting.client.HttpInvokerRequestExecutor;
import org.ironrhino.core.remoting.serializer.FstHttpInvokerSerializer;
import org.ironrhino.core.remoting.serializer.HttpInvokerSerializers;
import org.ironrhino.core.remoting.serializer.JavaHttpInvokerSerializer;
import org.ironrhino.core.servlet.AccessFilter;
import org.ironrhino.core.util.CodecUtils;
import org.ironrhino.sample.remoting.BarService;
import org.ironrhino.sample.remoting.FooService;
import org.ironrhino.sample.remoting.TestService;
import org.ironrhino.sample.remoting.TestService.FutureType;
import org.ironrhino.security.domain.User;
import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.concurrent.ListenableFuture;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;

@TestPropertySource(properties = "httpInvoker.serializationType=JAVA")
public class JavaHttpInvokerServerTest extends HttpInvokerServerTestBase {

	@Autowired
	private ServiceRegistry serviceRegistry;
	@Autowired
	private TestService testService;
	@Autowired
	private TestService mockTestService;
	@Autowired
	private FooService fooService;
	@Autowired
	private FooService mockFooService;
	@Autowired
	@Qualifier("&barService")
	private HttpInvokerClient barServiceClient;
	@Autowired
	@Qualifier("fallbackBarService")
	private BarService fallbackBarService;
	@Autowired
	private BarService mockBarService;
	@Autowired
	private HttpInvokerServer httpInvokerServer;
	@Autowired
	private HttpInvokerRequestExecutor mockHttpInvokerRequestExecutor;
	@Value("${httpInvoker.serializationType:}")
	private String serializationType;

	@After
	public void clearInvocations() {
		Mockito.clearInvocations(mockHttpInvokerRequestExecutor, mockTestService, mockFooService, mockBarService,
				fallbackBarService);
	}

	@Test
	public void testServiceRegistry() {
		Map<String, Object> exportedServices = serviceRegistry.getExportedServices();
		assertThat(exportedServices.containsKey(TestService.class.getName()), is(true));
		assertThat(exportedServices.containsKey(FooService.class.getName()), is(true));
		assertThat(exportedServices.get(TestService.class.getName()), is(mockTestService));
		assertThat(exportedServices.get(FooService.class.getName()), is(mockFooService));
	}

	@Test
	public void testServiceNotFound() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		URI uri = URI.create(serviceUrl(List.class)); // any interface not register as service
		request.setServerName(uri.getHost());
		request.setServerPort(uri.getPort());
		request.setRequestURI(uri.getPath());
		request.addHeader(HttpHeaders.CONTENT_TYPE,
				HttpInvokerSerializers.ofSerializationType(serializationType).getContentType());
		request.addHeader(AccessFilter.HTTP_HEADER_REQUEST_ID, CodecUtils.nextId());
		MockHttpServletResponse response = new MockHttpServletResponse();
		httpInvokerServer.handleRequest(request, response);
		assertThat(response.getStatus(), is(HttpServletResponse.SC_NOT_FOUND));
	}

	@Test
	public void testSerializationFailed() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		URI uri = URI.create(serviceUrl(TestService.class));
		request.setServerName(uri.getHost());
		request.setServerPort(uri.getPort());
		request.setRequestURI(uri.getPath());
		request.addHeader(HttpHeaders.CONTENT_TYPE,
				HttpInvokerSerializers.ofSerializationType(serializationType).getContentType());
		request.addHeader(AccessFilter.HTTP_HEADER_REQUEST_ID, CodecUtils.nextId());
		MockHttpServletResponse response = new MockHttpServletResponse();
		httpInvokerServer.handleRequest(request, response);
		if (HttpInvokerSerializers.DEFAULT_SERIALIZER.getSerializationType().equals(serializationType)) {
			assertThat(response.getStatus(), is(HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
		} else {
			assertThat(response.getStatus(), is(RemotingContext.SC_SERIALIZATION_FAILED));
		}
	}

	@Test
	public void testPing() throws Exception {
		testService.ping();
		then(mockHttpInvokerRequestExecutor).should().executeRequest(eq(serviceUrl(TestService.class)),
				argThat(ri -> "ping".equals(ri.getMethodName())), any(MethodInvocation.class));
		assertThat(mockHttpServletResponse.getStatus(), is(HttpServletResponse.SC_OK));
		then(mockTestService).should().ping();
	}

	@Test
	public void testEcho() throws Exception {
		assertThat(testService.echo(), is(""));
		then(mockHttpInvokerRequestExecutor).should().executeRequest(eq(serviceUrl(TestService.class)),
				argThat(ri -> "echo".equals(ri.getMethodName())), any(MethodInvocation.class));
		assertThat(mockHttpServletResponse.getStatus(), is(HttpServletResponse.SC_OK));
		then(mockTestService).should().echo();
		TestService.Immutable value = new TestService.Immutable(12, "test");
		assertThat(testService.echoImmutable(value), is(value));
		then(mockHttpInvokerRequestExecutor).should().executeRequest(eq(serviceUrl(TestService.class)),
				argThat(ri -> "echoImmutable".equals(ri.getMethodName())), any(MethodInvocation.class));
		assertThat(mockHttpServletResponse.getStatus(), is(HttpServletResponse.SC_OK));
		then(mockTestService).should().echoImmutable(value);
	}

	@Test
	public void testDefaultEcho() throws Exception {
		assertThat(testService.defaultEcho(""), is(""));
		then(mockTestService).should(never()).defaultEcho("");
		then(mockHttpInvokerRequestExecutor).should(never()).executeRequest(eq(serviceUrl(TestService.class)),
				argThat(ri -> "defaultEcho".equals(ri.getMethodName())), any(MethodInvocation.class));
		then(mockHttpInvokerRequestExecutor).should().executeRequest(eq(serviceUrl(TestService.class)),
				argThat(ri -> "echo".equals(ri.getMethodName())), any(MethodInvocation.class));
		assertThat(mockHttpServletResponse.getStatus(), is(HttpServletResponse.SC_OK));
		then(mockTestService).should().echo("");
	}

	@Test
	public void testEchoList() throws Exception {
		assertThat(testService.echoList(Arrays.asList("test")).get(0), is("test"));
		then(mockHttpInvokerRequestExecutor).should().executeRequest(eq(serviceUrl(TestService.class)),
				argThat(ri -> "echoList".equals(ri.getMethodName())), any(MethodInvocation.class));
		assertThat(mockHttpServletResponse.getStatus(), is(HttpServletResponse.SC_OK));
		then(mockTestService).should().echoList(argThat(list -> list.get(0).equals("test")));
	}

	@Test
	public void testEchoListWithArray() throws Exception {
		assertThat(testService.echoListWithArray(Collections.singletonList(new String[] { "test" })).get(0)[0],
				is("test"));
		then(mockHttpInvokerRequestExecutor).should().executeRequest(eq(serviceUrl(TestService.class)),
				argThat(ri -> "echoListWithArray".equals(ri.getMethodName())), any(MethodInvocation.class));
		assertThat(mockHttpServletResponse.getStatus(), is(HttpServletResponse.SC_OK));
		then(mockTestService).should().echoListWithArray(argThat(list -> list.get(0)[0].equals("test")));
	}

	@Test
	public void testEchoArray() throws Exception {
		assertThat(testService.echoArray(new String[] { "test" })[0], is("test"));
		then(mockHttpInvokerRequestExecutor).should().executeRequest(eq(serviceUrl(TestService.class)),
				argThat(ri -> "echoArray".equals(ri.getMethodName())), any(MethodInvocation.class));
		assertThat(mockHttpServletResponse.getStatus(), is(HttpServletResponse.SC_OK));
		then(mockTestService).should().echoArray(argThat(array -> array[0].equals("test")));
	}

	@Test
	public void testConcreteType() throws Exception {
		assertThat(testService.loadUserByUsername(null), is(nullValue()));
		then(mockTestService).should().loadUserByUsername(isNull());

		assertThat(testService.loadUserByUsername("username").getUsername(), is("username"));
		then(mockTestService).should().loadUserByUsername(eq("username"));

		assertThat(testService.searchUser(null), is(nullValue()));
		then(mockTestService).should().searchUser(isNull());
		assertThat(testService.searchUser(""), is(Collections.EMPTY_LIST));
		then(mockTestService).should().searchUser(eq(""));
		assertThat(testService.searchUser("username").get(0).getUsername(), is("username"));
		then(mockTestService).should().searchUser(eq("username"));
	}

	@Test
	public void testNonConcreteType() throws Exception {
		assertThat(testService.echoUserDetails(null), is(nullValue()));
		then(mockTestService).should().echoUserDetails(isNull());
		User user = new User();
		user.setUsername("test");
		UserDetails userDetails = testService.echoUserDetails(user);
		verifyUserDetails(userDetails);
		assertThat(userDetails.getUsername(), is("test"));
		then(mockTestService).should().echoUserDetails(argThat(u -> u != null && u.getUsername().equals("test")));

		assertThat(testService.loadUserDetailsByUsername(null), is(nullValue()));
		then(mockTestService).should().loadUserDetailsByUsername(isNull());
		userDetails = testService.loadUserDetailsByUsername("test");
		verifyUserDetails(userDetails);
		assertThat(userDetails.getUsername(), is("test"));
		then(mockTestService).should().loadUserDetailsByUsername(eq("test"));

		assertThat(testService.searchUserDetails(null), is(nullValue()));
		then(mockTestService).should().searchUserDetails(isNull());
		assertThat(testService.searchUserDetails(""), is(Collections.EMPTY_LIST));
		then(mockTestService).should().searchUserDetails(eq(""));
		List<? extends UserDetails> list = testService.searchUserDetails("test");
		verifyUserDetails(list.get(0));
		assertThat(list.get(0).getUsername(), is("test"));
		then(mockTestService).should().searchUserDetails(eq("test"));
	}

	@Test
	public void testBeanValidation() {
		assertThat(testService.echoScope(Scope.LOCAL), is(Scope.LOCAL));
		then(mockTestService).should().echoScope(Scope.LOCAL);

		Exception e = null;
		try {
			testService.echoScope(null);
		} catch (ConstraintViolationException error) {
			e = error;
		}
		assertThat(e, is(notNullValue()));
		assertThat(e instanceof ConstraintViolationException, is(true));
		then(mockTestService).should(never()).echoScope(isNull());
	}

	@Test
	public void testBeanValidationWithValid() {
		User user = new User();
		user.setEmail("test@test.com");
		assertThat(testService.echoUser(user), is(user));
		then(mockTestService).should().echoUser(user);

		user.setEmail("iamnotemail");
		Exception e = null;
		try {
			testService.echoUser(user);
		} catch (ConstraintViolationException error) {
			e = error;
		}
		assertThat(e, is(notNullValue()));
		assertThat(e instanceof ConstraintViolationException, is(true));
		then(mockTestService).should(never()).echoUser(user);
	}

	@Test
	public void testThrowException() throws Exception {
		boolean error = false;
		try {
			testService.throwException("");
		} catch (Exception e) {
			error = true;
		}
		assertThat(error, is(true));
		then(mockHttpInvokerRequestExecutor).should().executeRequest(eq(serviceUrl(TestService.class)),
				argThat(ri -> "throwException".equals(ri.getMethodName())), any(MethodInvocation.class));
		assertThat(mockHttpServletResponse.getStatus(), is(HttpServletResponse.SC_OK));
		then(mockTestService).should().throwException("");
	}

	@Test
	public void testOptional() {
		assertThat(testService.loadOptionalUserByUsername("").isPresent(), is(false));
		assertThat(mockHttpServletResponse.getStatus(), is(HttpServletResponse.SC_OK));
		then(mockTestService).should().loadOptionalUserByUsername("");

		assertThat(testService.loadOptionalUserByUsername("test").isPresent(), is(true));
		assertThat(mockHttpServletResponse.getStatus(), is(HttpServletResponse.SC_OK));
		then(mockTestService).should().loadOptionalUserByUsername("test");

		willReturn(null).given(mockTestService).loadOptionalUserByUsername(null);
		assertThat(mockTestService.loadOptionalUserByUsername(null), is(nullValue()));
		assertThat(testService.loadOptionalUserByUsername(null), is(notNullValue()));
		then(mockTestService).should(atLeast(2)).loadOptionalUserByUsername(null);
	}

	@Test
	public void testNonConcreteOptional() {
		assertThat(testService.loadOptionalUserDetailsByUsername("").isPresent(), is(false));
		assertThat(mockHttpServletResponse.getStatus(), is(HttpServletResponse.SC_OK));
		then(mockTestService).should().loadOptionalUserDetailsByUsername("");

		Optional<? extends UserDetails> userDetailsOptional = testService.loadOptionalUserDetailsByUsername("test");
		assertThat(userDetailsOptional.isPresent(), is(true));
		verifyUserDetails(userDetailsOptional.get());
		assertThat(mockHttpServletResponse.getStatus(), is(HttpServletResponse.SC_OK));
		then(mockTestService).should().loadOptionalUserDetailsByUsername("test");

		willReturn(null).given(mockTestService).loadOptionalUserDetailsByUsername(null);
		assertThat(mockTestService.loadOptionalUserDetailsByUsername(null), is(nullValue()));
		assertThat(testService.loadOptionalUserDetailsByUsername(null), is(notNullValue()));
		then(mockTestService).should(atLeast(2)).loadOptionalUserDetailsByUsername(null);
	}

	@Test
	public void testCallable() throws Exception {
		Callable<User> callable = testService.loadCallableUserByUsername("username");
		then(mockHttpInvokerRequestExecutor).shouldHaveNoMoreInteractions();
		then(mockTestService).shouldHaveNoMoreInteractions();

		assertThat(callable.call().getUsername(), is("username"));
		then(mockHttpInvokerRequestExecutor).should().executeRequest(eq(serviceUrl(TestService.class)),
				argThat(ri -> "loadCallableUserByUsername".equals(ri.getMethodName())), any(MethodInvocation.class));
		then(mockHttpServletRequest).should().startAsync();
		then(mockAsyncContext).should().start(any(Runnable.class));
		then(mockAsyncContext).should().complete();
		then(mockTestService).should().loadCallableUserByUsername("username");
	}

	@Test
	public void testNonConcreteCallable() throws Exception {
		Callable<? extends UserDetails> callable = testService.loadCallableUserDetailsByUsername("username");
		then(mockHttpInvokerRequestExecutor).shouldHaveNoMoreInteractions();
		then(mockTestService).shouldHaveNoMoreInteractions();

		UserDetails userDetails = callable.call();
		assertThat(userDetails.getUsername(), is("username"));
		verifyUserDetails(userDetails);
		then(mockHttpInvokerRequestExecutor).should().executeRequest(eq(serviceUrl(TestService.class)),
				argThat(ri -> "loadCallableUserDetailsByUsername".equals(ri.getMethodName())),
				any(MethodInvocation.class));
		then(mockHttpServletRequest).should().startAsync();
		then(mockAsyncContext).should().start(any(Runnable.class));
		then(mockAsyncContext).should().complete();
		then(mockTestService).should().loadCallableUserDetailsByUsername("username");
	}

	@Test
	public void testFuture() throws Exception {
		for (FutureType futureType : FutureType.values()) {
			Future<User> future = testService.loadFutureUserByUsername("username", futureType);
			assertThat(future.get().getUsername(), is("username"));
			then(mockHttpInvokerRequestExecutor).should().executeRequest(eq(serviceUrl(TestService.class)),
					argThat(ri -> "loadFutureUserByUsername".equals(ri.getMethodName())), any(MethodInvocation.class));
			then(mockHttpServletRequest).should().startAsync();
			if (futureType == FutureType.RUNNABLE)
				then(mockAsyncContext).should().start(any(Runnable.class));
			then(mockAsyncContext).should().complete();
			then(mockTestService).should().loadFutureUserByUsername("username", futureType);
			clearInvocations();
		}
	}

	@Test
	public void testNonConcreteFuture() throws Exception {
		for (FutureType futureType : FutureType.values()) {
			Future<? extends UserDetails> future = testService.loadFutureUserDetailsByUsername("username", futureType);
			UserDetails userDetails = future.get();
			assertThat(userDetails.getUsername(), is("username"));
			verifyUserDetails(userDetails);
			then(mockHttpInvokerRequestExecutor).should().executeRequest(eq(serviceUrl(TestService.class)),
					argThat(ri -> "loadFutureUserDetailsByUsername".equals(ri.getMethodName())),
					any(MethodInvocation.class));
			then(mockHttpServletRequest).should().startAsync();
			if (futureType == FutureType.RUNNABLE)
				then(mockAsyncContext).should().start(any(Runnable.class));
			then(mockAsyncContext).should().complete();
			then(mockTestService).should().loadFutureUserDetailsByUsername("username", futureType);
			clearInvocations();
		}
	}

	@Test
	public void testFutureWithNullUsername() throws Exception {
		for (FutureType futureType : FutureType.values()) {
			boolean error = false;
			try {
				testService.loadFutureUserByUsername(null, futureType).get();
			} catch (ExecutionException e) {
				assertThat(e.getCause() instanceof IllegalArgumentException, is(true));
				error = true;
			}
			assertThat(error, is(true));
			then(mockHttpInvokerRequestExecutor).should().executeRequest(eq(serviceUrl(TestService.class)),
					argThat(ri -> "loadFutureUserByUsername".equals(ri.getMethodName())), any(MethodInvocation.class));
			then(mockTestService).should().loadFutureUserByUsername(null, futureType);
			then(mockHttpServletRequest).should(never()).startAsync();
			then(mockAsyncContext).shouldHaveNoMoreInteractions();
			clearInvocations();
		}
	}

	@Test
	public void testFutureWithBlankUsername() throws Exception {
		for (FutureType futureType : FutureType.values()) {
			boolean error = false;
			try {
				testService.loadFutureUserByUsername("", futureType).get();
			} catch (ExecutionException e) {
				assertThat(e.getCause() instanceof IllegalArgumentException, is(true));
				error = true;
			}
			assertThat(error, is(true));
			then(mockHttpInvokerRequestExecutor).should().executeRequest(eq(serviceUrl(TestService.class)),
					argThat(ri -> "loadFutureUserByUsername".equals(ri.getMethodName())), any(MethodInvocation.class));
			then(mockHttpServletRequest).should().startAsync();
			if (futureType == FutureType.RUNNABLE)
				then(mockAsyncContext).should().start(any(Runnable.class));
			then(mockAsyncContext).should().complete();
			then(mockTestService).should().loadFutureUserByUsername("", futureType);
			clearInvocations();
		}
	}

	@Test
	public void testListenableFuture() throws Exception {
		ListenableFuture<User> listenableFuture = testService.loadListenableFutureUserByUsername("username");
		AtomicBoolean b1 = new AtomicBoolean();
		AtomicBoolean b2 = new AtomicBoolean();
		listenableFuture.addCallback(u -> b1.set(u != null && "username".equals(u.getUsername())), e -> b2.set(true));
		Thread.sleep(1000);
		assertThat(b1.get(), is(true));
		assertThat(b2.get(), is(false));
		assertThat(listenableFuture.get().getUsername(), is("username"));
		then(mockHttpInvokerRequestExecutor).should().executeRequest(eq(serviceUrl(TestService.class)),
				argThat(ri -> "loadListenableFutureUserByUsername".equals(ri.getMethodName())),
				any(MethodInvocation.class));
		then(mockHttpServletRequest).should().startAsync();
		then(mockAsyncContext).should().complete();
		then(mockTestService).should().loadListenableFutureUserByUsername("username");
	}

	@Test
	public void testNonConcreteListenableFuture() throws Exception {
		ListenableFuture<? extends UserDetails> listenableFuture = testService
				.loadListenableFutureUserDetailsByUsername("username");
		AtomicBoolean b1 = new AtomicBoolean();
		AtomicBoolean b2 = new AtomicBoolean();
		listenableFuture.addCallback(u -> b1.set(u != null && "username".equals(u.getUsername())), e -> b2.set(true));
		Thread.sleep(1000);
		assertThat(b1.get(), is(true));
		assertThat(b2.get(), is(false));
		UserDetails userDetails = listenableFuture.get();
		assertThat(userDetails.getUsername(), is("username"));
		verifyUserDetails(userDetails);
		then(mockHttpInvokerRequestExecutor).should().executeRequest(eq(serviceUrl(TestService.class)),
				argThat(ri -> "loadListenableFutureUserDetailsByUsername".equals(ri.getMethodName())),
				any(MethodInvocation.class));
		then(mockHttpServletRequest).should().startAsync();
		then(mockAsyncContext).should().complete();
		then(mockTestService).should().loadListenableFutureUserDetailsByUsername("username");
	}

	@Test
	public void testCompletableFuture() throws Exception {
		CompletableFuture<User> completableFuture = testService.loadCompletableFutureUserByUsername("username");
		assertThat(completableFuture.get().getUsername(), is("username"));
		then(mockHttpInvokerRequestExecutor).should().executeRequest(eq(serviceUrl(TestService.class)),
				argThat(ri -> "loadCompletableFutureUserByUsername".equals(ri.getMethodName())),
				any(MethodInvocation.class));
		then(mockHttpServletRequest).should().startAsync();
		then(mockAsyncContext).should().complete();
		then(mockTestService).should().loadCompletableFutureUserByUsername("username");
	}

	@Test
	public void testNonConcreteCompletableFuture() throws Exception {
		CompletableFuture<? extends UserDetails> completableFuture = testService
				.loadCompletableFutureUserDetailsByUsername("username");
		assertThat(completableFuture.get().getUsername(), is("username"));
		then(mockHttpInvokerRequestExecutor).should().executeRequest(eq(serviceUrl(TestService.class)),
				argThat(ri -> "loadCompletableFutureUserDetailsByUsername".equals(ri.getMethodName())),
				any(MethodInvocation.class));
		then(mockHttpServletRequest).should().startAsync();
		then(mockAsyncContext).should().complete();
		then(mockTestService).should().loadCompletableFutureUserDetailsByUsername("username");
	}

	@Test
	public void testAttempt() throws Exception {
		final int maxAttempts = 5;
		barServiceClient.setMaxAttempts(maxAttempts);
		willThrow(new Exception("test")).given(mockHttpInvokerRequestExecutor).executeRequest(
				contains(BarService.class.getName()), argThat(ri -> "test".equals(ri.getMethodName())),
				any(MethodInvocation.class));
		boolean error = false;
		try {
			((BarService) (barServiceClient.getObject())).test("");
		} catch (Throwable e) {
			error = true;
		}
		assertThat(error, is(true));
		then(mockHttpInvokerRequestExecutor).should(times(maxAttempts)).executeRequest(
				contains(BarService.class.getName()), argThat(ri -> "test".equals(ri.getMethodName())),
				any(MethodInvocation.class));
		then(mockBarService).should(never()).test(eq(""));
	}

	@Test
	public void testSerializationDowngrade() throws Exception {
		String defaultSerializer = HttpInvokerSerializers.DEFAULT_SERIALIZER.getSerializationType();
		if (!defaultSerializer.equals(serializationType)) {
			final int maxAttempts = 5;
			barServiceClient.setMaxAttempts(maxAttempts);
			willThrow(new SerializationFailedException("test")).given(mockHttpInvokerRequestExecutor).executeRequest(
					contains(BarService.class.getName()), argThat(ri -> "test".contentEquals(ri.getMethodName())),
					any(MethodInvocation.class));
			boolean error = false;
			try {
				((BarService) (barServiceClient.getObject())).test("");
			} catch (Throwable e) {
				error = true;
			}
			assertThat(error, is(true));
			assertThat(mockHttpInvokerRequestExecutor.getSerializer().getSerializationType(), is(defaultSerializer));
			then(mockHttpInvokerRequestExecutor).should().setSerializer(HttpInvokerSerializers.DEFAULT_SERIALIZER);
			then(mockHttpInvokerRequestExecutor).should(times(maxAttempts)).executeRequest(
					contains(BarService.class.getName()), argThat(ri -> "test".contentEquals(ri.getMethodName())),
					any(MethodInvocation.class));
			then(mockBarService).should(never()).test(eq(""));
			mockHttpInvokerRequestExecutor.setSerializer(HttpInvokerSerializers.ofSerializationType(serializationType));
		}
	}

	@Test
	public void testFallbackWithServiceNotFound() throws Exception {
		final int maxAttempts = 1;
		barServiceClient.setMaxAttempts(maxAttempts);
		willThrow(new ServiceNotFoundException(BarService.class.getName())).given(mockHttpInvokerRequestExecutor)
				.executeRequest(contains(BarService.class.getName()),
						argThat(ri -> "test".contentEquals(ri.getMethodName())), any(MethodInvocation.class));
		assertThat(((BarService) (barServiceClient.getObject())).test(""), is("fallback:"));
		then(mockBarService).should(never()).test(eq(""));
		then(fallbackBarService).should().test(eq(""));
	}

	@Test
	public void testFallbackWithCircuitBreakerOpenException() throws Exception {
		final int maxAttempts = 1;
		barServiceClient.setMaxAttempts(maxAttempts);
		willThrow(CallNotPermittedException.createCallNotPermittedException(CircuitBreaker.ofDefaults("test")))
				.given(mockHttpInvokerRequestExecutor).executeRequest(contains(BarService.class.getName()),
						argThat(ri -> "test".contentEquals(ri.getMethodName())), any(MethodInvocation.class));
		assertThat(((BarService) (barServiceClient.getObject())).test(""), is("fallback:"));
		then(mockBarService).should(never()).test(eq(""));
		then(fallbackBarService).should().test(eq(""));
	}

	@Test
	public void testServiceImplementedByFactoryBean() throws Exception {
		fooService.test("test");
		then(mockHttpInvokerRequestExecutor).should().executeRequest(eq(serviceUrl(FooService.class)),
				argThat(ri -> "test".equals(ri.getMethodName())), any(MethodInvocation.class));
		assertThat(mockHttpServletResponse.getStatus(), is(HttpServletResponse.SC_OK));
		then(mockFooService).should().test("test");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testChaos() throws Exception {
		fooService.test("chaos");
	}

	protected void verifyUserDetails(UserDetails userDetails) {
		if (FstHttpInvokerSerializer.INSTANCE.getSerializationType().equals(serializationType)
				|| JavaHttpInvokerSerializer.INSTANCE.getSerializationType().equals(serializationType)) {
			assertThat(userDetails instanceof User, is(true));
		} else {
			assertThat(userDetails instanceof User, is(false));
		}
	}

}