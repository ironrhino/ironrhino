package org.ironrhino.core.remoting.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
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

import io.github.resilience4j.circuitbreaker.CircuitBreakerOpenException;

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
		assertTrue(exportedServices.containsKey(TestService.class.getName()));
		assertTrue(exportedServices.containsKey(FooService.class.getName()));
		assertSame(mockTestService, exportedServices.get(TestService.class.getName()));
		assertSame(mockFooService, exportedServices.get(FooService.class.getName()));
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
		assertEquals(HttpServletResponse.SC_NOT_FOUND, response.getStatus());
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
			assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		} else {
			assertEquals(RemotingContext.SC_SERIALIZATION_FAILED, response.getStatus());
		}
	}

	@Test
	public void testPing() throws Exception {
		testService.ping();
		then(mockHttpInvokerRequestExecutor).should().executeRequest(eq(serviceUrl(TestService.class)),
				argThat(ri -> "ping".equals(ri.getMethodName())), any(MethodInvocation.class));
		assertEquals(HttpServletResponse.SC_OK, mockHttpServletResponse.getStatus());
		then(mockTestService).should().ping();
	}

	@Test
	public void testEcho() throws Exception {
		assertEquals("", testService.echo());
		then(mockHttpInvokerRequestExecutor).should().executeRequest(eq(serviceUrl(TestService.class)),
				argThat(ri -> "echo".equals(ri.getMethodName())), any(MethodInvocation.class));
		assertEquals(HttpServletResponse.SC_OK, mockHttpServletResponse.getStatus());
		then(mockTestService).should().echo();
		TestService.Immutable value = new TestService.Immutable(12, "test");
		assertEquals(value, testService.echoImmutable(value));
		then(mockHttpInvokerRequestExecutor).should().executeRequest(eq(serviceUrl(TestService.class)),
				argThat(ri -> "echoImmutable".equals(ri.getMethodName())), any(MethodInvocation.class));
		assertEquals(HttpServletResponse.SC_OK, mockHttpServletResponse.getStatus());
		then(mockTestService).should().echoImmutable(value);
	}

	@Test
	public void testDefaultEcho() throws Exception {
		assertEquals("", testService.defaultEcho(""));
		then(mockTestService).should(never()).defaultEcho("");
		then(mockHttpInvokerRequestExecutor).should(never()).executeRequest(eq(serviceUrl(TestService.class)),
				argThat(ri -> "defaultEcho".equals(ri.getMethodName())), any(MethodInvocation.class));
		then(mockHttpInvokerRequestExecutor).should().executeRequest(eq(serviceUrl(TestService.class)),
				argThat(ri -> "echo".equals(ri.getMethodName())), any(MethodInvocation.class));
		assertEquals(HttpServletResponse.SC_OK, mockHttpServletResponse.getStatus());
		then(mockTestService).should().echo("");
	}

	@Test
	public void testEchoList() throws Exception {
		assertEquals("test", testService.echoList(Arrays.asList("test")).get(0));
		then(mockHttpInvokerRequestExecutor).should().executeRequest(eq(serviceUrl(TestService.class)),
				argThat(ri -> "echoList".equals(ri.getMethodName())), any(MethodInvocation.class));
		assertEquals(HttpServletResponse.SC_OK, mockHttpServletResponse.getStatus());
		then(mockTestService).should().echoList(argThat(list -> list.get(0).equals("test")));
	}

	@Test
	public void testEchoListWithArray() throws Exception {
		assertEquals("test",
				testService.echoListWithArray(Collections.singletonList(new String[] { "test" })).get(0)[0]);
		then(mockHttpInvokerRequestExecutor).should().executeRequest(eq(serviceUrl(TestService.class)),
				argThat(ri -> "echoListWithArray".equals(ri.getMethodName())), any(MethodInvocation.class));
		assertEquals(HttpServletResponse.SC_OK, mockHttpServletResponse.getStatus());
		then(mockTestService).should().echoListWithArray(argThat(list -> list.get(0)[0].equals("test")));
	}

	@Test
	public void testEchoArray() throws Exception {
		assertEquals("test", testService.echoArray(new String[] { "test" })[0]);
		then(mockHttpInvokerRequestExecutor).should().executeRequest(eq(serviceUrl(TestService.class)),
				argThat(ri -> "echoArray".equals(ri.getMethodName())), any(MethodInvocation.class));
		assertEquals(HttpServletResponse.SC_OK, mockHttpServletResponse.getStatus());
		then(mockTestService).should().echoArray(argThat(array -> array[0].equals("test")));
	}

	@Test
	public void testContcreteType() throws Exception {
		assertNull(testService.loadUserByUsername(null));
		then(mockTestService).should().loadUserByUsername(isNull());

		assertEquals("username", testService.loadUserByUsername("username").getUsername());
		then(mockTestService).should().loadUserByUsername(eq("username"));

		assertNull(testService.searchUser(null));
		then(mockTestService).should().searchUser(isNull());
		assertEquals(Collections.EMPTY_LIST, testService.searchUser(""));
		then(mockTestService).should().searchUser(eq(""));
		assertEquals("username", testService.searchUser("username").get(0).getUsername());
		then(mockTestService).should().searchUser(eq("username"));
	}

	@Test
	public void testNonContcreteType() throws Exception {
		assertNull(testService.echoUserDetails(null));
		then(mockTestService).should().echoUserDetails(isNull());
		User user = new User();
		user.setUsername("test");
		UserDetails userDetails = testService.echoUserDetails(user);
		verifyUserDetails(userDetails);
		assertEquals("test", userDetails.getUsername());
		then(mockTestService).should().echoUserDetails(argThat(u -> u != null && u.getUsername().equals("test")));

		assertNull(testService.loadUserDetailsByUsername(null));
		then(mockTestService).should().loadUserDetailsByUsername(isNull());
		userDetails = testService.loadUserDetailsByUsername("test");
		verifyUserDetails(userDetails);
		assertEquals("test", userDetails.getUsername());
		then(mockTestService).should().loadUserDetailsByUsername(eq("test"));

		assertNull(testService.searchUserDetails(null));
		then(mockTestService).should().searchUserDetails(isNull());
		assertEquals(Collections.EMPTY_LIST, testService.searchUserDetails(""));
		then(mockTestService).should().searchUserDetails(eq(""));
		List<? extends UserDetails> list = testService.searchUserDetails("test");
		verifyUserDetails(list.get(0));
		assertEquals("test", list.get(0).getUsername());
		then(mockTestService).should().searchUserDetails(eq("test"));
	}

	@Test
	public void testBeanValidation() {
		assertEquals(Scope.LOCAL, testService.echoScope(Scope.LOCAL));
		then(mockTestService).should().echoScope(Scope.LOCAL);

		Exception e = null;
		try {
			testService.echoScope(null);
		} catch (ConstraintViolationException error) {
			e = error;
		}
		assertNotNull(e);
		assertTrue(e instanceof ConstraintViolationException);
		then(mockTestService).should(never()).echoScope(isNull());
	}

	@Test
	public void testBeanValidationWithValid() {
		User user = new User();
		user.setEmail("test@test.com");
		assertEquals(user, testService.echoUser(user));
		then(mockTestService).should().echoUser(user);

		user.setEmail("iamnotemail");
		Exception e = null;
		try {
			testService.echoUser(user);
		} catch (ConstraintViolationException error) {
			e = error;
		}
		assertNotNull(e);
		assertTrue(e instanceof ConstraintViolationException);
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
		assertTrue(error);
		then(mockHttpInvokerRequestExecutor).should().executeRequest(eq(serviceUrl(TestService.class)),
				argThat(ri -> "throwException".equals(ri.getMethodName())), any(MethodInvocation.class));
		assertEquals(HttpServletResponse.SC_OK, mockHttpServletResponse.getStatus());
		then(mockTestService).should().throwException("");
	}

	@Test
	public void testOptional() {
		assertFalse(testService.loadOptionalUserByUsername("").isPresent());
		assertEquals(HttpServletResponse.SC_OK, mockHttpServletResponse.getStatus());
		then(mockTestService).should().loadOptionalUserByUsername("");

		assertTrue(testService.loadOptionalUserByUsername("test").isPresent());
		assertEquals(HttpServletResponse.SC_OK, mockHttpServletResponse.getStatus());
		then(mockTestService).should().loadOptionalUserByUsername("test");

		willReturn(null).given(mockTestService).loadOptionalUserByUsername(null);
		assertNull(mockTestService.loadOptionalUserByUsername(null));
		assertNotNull(testService.loadOptionalUserByUsername(null));
		then(mockTestService).should(atLeast(2)).loadOptionalUserByUsername(null);
	}

	@Test
	public void testNonConcreteOptional() {
		assertFalse(testService.loadOptionalUserDetailsByUsername("").isPresent());
		assertEquals(HttpServletResponse.SC_OK, mockHttpServletResponse.getStatus());
		then(mockTestService).should().loadOptionalUserDetailsByUsername("");

		Optional<? extends UserDetails> userDetailsOptional = testService.loadOptionalUserDetailsByUsername("test");
		assertTrue(userDetailsOptional.isPresent());
		verifyUserDetails(userDetailsOptional.get());
		assertEquals(HttpServletResponse.SC_OK, mockHttpServletResponse.getStatus());
		then(mockTestService).should().loadOptionalUserDetailsByUsername("test");

		willReturn(null).given(mockTestService).loadOptionalUserDetailsByUsername(null);
		assertNull(mockTestService.loadOptionalUserDetailsByUsername(null));
		assertNotNull(testService.loadOptionalUserDetailsByUsername(null));
		then(mockTestService).should(atLeast(2)).loadOptionalUserDetailsByUsername(null);
	}

	@Test
	public void testCallable() throws Exception {
		Callable<User> callable = testService.loadCallableUserByUsername("username");
		then(mockHttpInvokerRequestExecutor).shouldHaveZeroInteractions();
		then(mockTestService).shouldHaveZeroInteractions();

		assertEquals("username", callable.call().getUsername());
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
		then(mockHttpInvokerRequestExecutor).shouldHaveZeroInteractions();
		then(mockTestService).shouldHaveZeroInteractions();

		UserDetails userDetails = callable.call();
		assertEquals("username", userDetails.getUsername());
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
			assertEquals("username", future.get().getUsername());
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
			assertEquals("username", userDetails.getUsername());
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
				assertTrue(e.getCause() instanceof IllegalArgumentException);
				error = true;
			}
			assertTrue(error);
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
				assertTrue(e.getCause() instanceof IllegalArgumentException);
				error = true;
			}
			assertTrue(error);
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
		assertTrue(b1.get());
		assertFalse(b2.get());
		assertEquals("username", listenableFuture.get().getUsername());
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
		assertTrue(b1.get());
		assertFalse(b2.get());
		UserDetails userDetails = listenableFuture.get();
		assertEquals("username", userDetails.getUsername());
		verifyUserDetails(userDetails);
		then(mockHttpInvokerRequestExecutor).should().executeRequest(eq(serviceUrl(TestService.class)),
				argThat(ri -> "loadListenableFutureUserDetailsByUsername".equals(ri.getMethodName())),
				any(MethodInvocation.class));
		then(mockHttpServletRequest).should().startAsync();
		then(mockAsyncContext).should().complete();
		then(mockTestService).should().loadListenableFutureUserDetailsByUsername("username");
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
		assertTrue(error);
		then(mockHttpInvokerRequestExecutor).should(times(maxAttempts)).executeRequest(
				contains(BarService.class.getName()), argThat(ri -> "test".equals(ri.getMethodName())),
				any(MethodInvocation.class));
		then(mockBarService).should(never()).test(eq(""));
	}

	@Test
	public void testSerializationDowngrade() throws Exception {
		String defaultSerizliazer = HttpInvokerSerializers.DEFAULT_SERIALIZER.getSerializationType();
		if (!defaultSerizliazer.equals(serializationType)) {
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
			assertTrue(error);
			assertEquals(defaultSerizliazer, mockHttpInvokerRequestExecutor.getSerializer().getSerializationType());
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
		assertEquals("fallback:", ((BarService) (barServiceClient.getObject())).test(""));
		then(mockBarService).should(never()).test(eq(""));
		then(fallbackBarService).should().test(eq(""));
	}

	@Test
	public void testFallbackWithCircuitBreakerOpenException() throws Exception {
		final int maxAttempts = 1;
		barServiceClient.setMaxAttempts(maxAttempts);
		willThrow(new CircuitBreakerOpenException("CircuitBreakerOpen")).given(mockHttpInvokerRequestExecutor)
				.executeRequest(contains(BarService.class.getName()),
						argThat(ri -> "test".contentEquals(ri.getMethodName())), any(MethodInvocation.class));
		assertEquals("fallback:", ((BarService) (barServiceClient.getObject())).test(""));
		then(mockBarService).should(never()).test(eq(""));
		then(fallbackBarService).should().test(eq(""));
	}

	@Test
	public void testServiceImplementedByFactoryBean() throws Exception {
		fooService.test("test");
		then(mockHttpInvokerRequestExecutor).should().executeRequest(eq(serviceUrl(FooService.class)),
				argThat(ri -> "test".equals(ri.getMethodName())), any(MethodInvocation.class));
		assertEquals(HttpServletResponse.SC_OK, mockHttpServletResponse.getStatus());
		then(mockFooService).should().test("test");
	}

	protected void verifyUserDetails(UserDetails userDetails) {
		if (FstHttpInvokerSerializer.INSTANCE.getSerializationType().equals(serializationType)
				|| JavaHttpInvokerSerializer.INSTANCE.getSerializationType().equals(serializationType)) {
			assertTrue(userDetails instanceof User);
		} else {
			assertFalse(userDetails instanceof User);
		}
	}

}