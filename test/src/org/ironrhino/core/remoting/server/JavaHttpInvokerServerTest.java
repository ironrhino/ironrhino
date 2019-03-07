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
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

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
		verify(mockHttpInvokerRequestExecutor).executeRequest(eq(serviceUrl(TestService.class)),
				argThat(ri -> "ping".equals(ri.getMethodName())), any(MethodInvocation.class));
		assertEquals(HttpServletResponse.SC_OK, mockHttpServletResponse.getStatus());
		verify(mockTestService).ping();
	}

	@Test
	public void testEcho() throws Exception {
		assertEquals("", testService.echo());
		verify(mockHttpInvokerRequestExecutor).executeRequest(eq(serviceUrl(TestService.class)),
				argThat(ri -> "echo".equals(ri.getMethodName())), any(MethodInvocation.class));
		assertEquals(HttpServletResponse.SC_OK, mockHttpServletResponse.getStatus());
		verify(mockTestService).echo();
	}

	@Test
	public void testDefaultEcho() throws Exception {
		assertEquals("", testService.defaultEcho(""));
		verify(mockTestService, Mockito.never()).defaultEcho("");
		verify(mockHttpInvokerRequestExecutor, Mockito.never()).executeRequest(eq(serviceUrl(TestService.class)),
				argThat(ri -> "defaultEcho".equals(ri.getMethodName())), any(MethodInvocation.class));
		verify(mockHttpInvokerRequestExecutor).executeRequest(eq(serviceUrl(TestService.class)),
				argThat(ri -> "echo".equals(ri.getMethodName())), any(MethodInvocation.class));
		assertEquals(HttpServletResponse.SC_OK, mockHttpServletResponse.getStatus());
		verify(mockTestService).echo("");
	}

	@Test
	public void testEchoList() throws Exception {
		assertEquals("test", testService.echoList(Arrays.asList("test")).get(0));
		verify(mockHttpInvokerRequestExecutor).executeRequest(eq(serviceUrl(TestService.class)),
				argThat(ri -> "echoList".equals(ri.getMethodName())), any(MethodInvocation.class));
		assertEquals(HttpServletResponse.SC_OK, mockHttpServletResponse.getStatus());
		verify(mockTestService).echoList(argThat(list -> list.get(0).equals("test")));
	}

	@Test
	public void testEchoListWithArray() throws Exception {
		assertEquals("test",
				testService.echoListWithArray(Collections.singletonList(new String[] { "test" })).get(0)[0]);
		verify(mockHttpInvokerRequestExecutor).executeRequest(eq(serviceUrl(TestService.class)),
				argThat(ri -> "echoListWithArray".equals(ri.getMethodName())), any(MethodInvocation.class));
		assertEquals(HttpServletResponse.SC_OK, mockHttpServletResponse.getStatus());
		verify(mockTestService).echoListWithArray(argThat(list -> list.get(0)[0].equals("test")));
	}

	@Test
	public void testEchoArray() throws Exception {
		assertEquals("test", testService.echoArray(new String[] { "test" })[0]);
		verify(mockHttpInvokerRequestExecutor).executeRequest(eq(serviceUrl(TestService.class)),
				argThat(ri -> "echoArray".equals(ri.getMethodName())), any(MethodInvocation.class));
		assertEquals(HttpServletResponse.SC_OK, mockHttpServletResponse.getStatus());
		verify(mockTestService).echoArray(argThat(array -> array[0].equals("test")));
	}

	@Test
	public void testContcreteType() throws Exception {
		assertNull(testService.loadUserByUsername(null));
		verify(mockTestService).loadUserByUsername(isNull());

		assertEquals("username", testService.loadUserByUsername("username").getUsername());
		verify(mockTestService).loadUserByUsername(eq("username"));

		assertNull(testService.searchUser(null));
		verify(mockTestService).searchUser(isNull());
		assertEquals(Collections.EMPTY_LIST, testService.searchUser(""));
		verify(mockTestService).searchUser(eq(""));
		assertEquals("username", testService.searchUser("username").get(0).getUsername());
		verify(mockTestService).searchUser(eq("username"));
	}

	@Test
	public void testNonContcreteType() throws Exception {
		assertNull(testService.echoUserDetails(null));
		verify(mockTestService).echoUserDetails(isNull());
		User user = new User();
		user.setUsername("test");
		UserDetails userDetails = testService.echoUserDetails(user);
		verifyUserDetails(userDetails);
		assertEquals("test", userDetails.getUsername());
		verify(mockTestService).echoUserDetails(argThat(u -> u != null && u.getUsername().equals("test")));

		assertNull(testService.loadUserDetailsByUsername(null));
		verify(mockTestService).loadUserDetailsByUsername(isNull());
		userDetails = testService.loadUserDetailsByUsername("test");
		verifyUserDetails(userDetails);
		assertEquals("test", userDetails.getUsername());
		verify(mockTestService).loadUserDetailsByUsername(eq("test"));

		assertNull(testService.searchUserDetails(null));
		verify(mockTestService).searchUserDetails(isNull());
		assertEquals(Collections.EMPTY_LIST, testService.searchUserDetails(""));
		verify(mockTestService).searchUserDetails(eq(""));
		List<? extends UserDetails> list = testService.searchUserDetails("test");
		verifyUserDetails(list.get(0));
		assertEquals("test", list.get(0).getUsername());
		verify(mockTestService).searchUserDetails(eq("test"));
	}

	@Test
	public void testBeanValidation() {
		assertEquals(Scope.LOCAL, testService.echoScope(Scope.LOCAL));
		verify(mockTestService).echoScope(Scope.LOCAL);

		Exception e = null;
		try {
			testService.echoScope(null);
		} catch (ConstraintViolationException error) {
			e = error;
		}
		assertNotNull(e);
		assertTrue(e instanceof ConstraintViolationException);
		verify(mockTestService, never()).echoScope(isNull());
	}

	@Test
	public void testBeanValidationWithValid() {
		User user = new User();
		user.setEmail("test@test.com");
		assertEquals(user, testService.echoUser(user));
		verify(mockTestService).echoUser(user);

		user.setEmail("iamnotemail");
		Exception e = null;
		try {
			testService.echoUser(user);
		} catch (ConstraintViolationException error) {
			e = error;
		}
		assertNotNull(e);
		assertTrue(e instanceof ConstraintViolationException);
		verify(mockTestService, never()).echoUser(user);
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
		verify(mockHttpInvokerRequestExecutor).executeRequest(eq(serviceUrl(TestService.class)),
				argThat(ri -> "throwException".equals(ri.getMethodName())), any(MethodInvocation.class));
		assertEquals(HttpServletResponse.SC_OK, mockHttpServletResponse.getStatus());
		verify(mockTestService).throwException("");
	}

	@Test
	public void testOptional() {
		assertFalse(testService.loadOptionalUserByUsername("").isPresent());
		assertEquals(HttpServletResponse.SC_OK, mockHttpServletResponse.getStatus());
		verify(mockTestService).loadOptionalUserByUsername("");

		assertTrue(testService.loadOptionalUserByUsername("test").isPresent());
		assertEquals(HttpServletResponse.SC_OK, mockHttpServletResponse.getStatus());
		verify(mockTestService).loadOptionalUserByUsername("test");

		doReturn(null).when(mockTestService).loadOptionalUserByUsername(null);
		assertNull(mockTestService.loadOptionalUserByUsername(null));
		assertNotNull(testService.loadOptionalUserByUsername(null));
		verify(mockTestService, atLeast(2)).loadOptionalUserByUsername(null);
	}

	@Test
	public void testNonConcreteOptional() {
		assertFalse(testService.loadOptionalUserDetailsByUsername("").isPresent());
		assertEquals(HttpServletResponse.SC_OK, mockHttpServletResponse.getStatus());
		verify(mockTestService).loadOptionalUserDetailsByUsername("");

		Optional<? extends UserDetails> userDetailsOptional = testService.loadOptionalUserDetailsByUsername("test");
		assertTrue(userDetailsOptional.isPresent());
		verifyUserDetails(userDetailsOptional.get());
		assertEquals(HttpServletResponse.SC_OK, mockHttpServletResponse.getStatus());
		verify(mockTestService).loadOptionalUserDetailsByUsername("test");

		doReturn(null).when(mockTestService).loadOptionalUserDetailsByUsername(null);
		assertNull(mockTestService.loadOptionalUserDetailsByUsername(null));
		assertNotNull(testService.loadOptionalUserDetailsByUsername(null));
		verify(mockTestService, atLeast(2)).loadOptionalUserDetailsByUsername(null);
	}

	@Test
	public void testCallable() throws Exception {
		Callable<User> callable = testService.loadCallableUserByUsername("username");
		verifyNoMoreInteractions(mockHttpInvokerRequestExecutor);
		verifyNoMoreInteractions(mockTestService);

		assertEquals("username", callable.call().getUsername());
		verify(mockHttpInvokerRequestExecutor).executeRequest(eq(serviceUrl(TestService.class)),
				argThat(ri -> "loadCallableUserByUsername".equals(ri.getMethodName())), any(MethodInvocation.class));
		verify(mockHttpServletRequest).startAsync();
		verify(mockAsyncContext).start(any(Runnable.class));
		verify(mockAsyncContext).complete();
		verify(mockTestService).loadCallableUserByUsername("username");
	}

	@Test
	public void testNonConcreteCallable() throws Exception {
		Callable<? extends UserDetails> callable = testService.loadCallableUserDetailsByUsername("username");
		verifyNoMoreInteractions(mockHttpInvokerRequestExecutor);
		verifyNoMoreInteractions(mockTestService);

		UserDetails userDetails = callable.call();
		assertEquals("username", userDetails.getUsername());
		verifyUserDetails(userDetails);
		verify(mockHttpInvokerRequestExecutor).executeRequest(eq(serviceUrl(TestService.class)),
				argThat(ri -> "loadCallableUserDetailsByUsername".equals(ri.getMethodName())),
				any(MethodInvocation.class));
		verify(mockHttpServletRequest).startAsync();
		verify(mockAsyncContext).start(any(Runnable.class));
		verify(mockAsyncContext).complete();
		verify(mockTestService).loadCallableUserDetailsByUsername("username");
	}

	@Test
	public void testFuture() throws Exception {
		for (FutureType futureType : FutureType.values()) {
			Future<User> future = testService.loadFutureUserByUsername("username", futureType);
			assertEquals("username", future.get().getUsername());
			verify(mockHttpInvokerRequestExecutor).executeRequest(eq(serviceUrl(TestService.class)),
					argThat(ri -> "loadFutureUserByUsername".equals(ri.getMethodName())), any(MethodInvocation.class));
			verify(mockHttpServletRequest).startAsync();
			if (futureType == FutureType.RUNNABLE)
				verify(mockAsyncContext).start(any(Runnable.class));
			verify(mockAsyncContext).complete();
			verify(mockTestService).loadFutureUserByUsername("username", futureType);
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
			verify(mockHttpInvokerRequestExecutor).executeRequest(eq(serviceUrl(TestService.class)),
					argThat(ri -> "loadFutureUserDetailsByUsername".equals(ri.getMethodName())),
					any(MethodInvocation.class));
			verify(mockHttpServletRequest).startAsync();
			if (futureType == FutureType.RUNNABLE)
				verify(mockAsyncContext).start(any(Runnable.class));
			verify(mockAsyncContext).complete();
			verify(mockTestService).loadFutureUserDetailsByUsername("username", futureType);
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
			verify(mockHttpInvokerRequestExecutor).executeRequest(eq(serviceUrl(TestService.class)),
					argThat(ri -> "loadFutureUserByUsername".equals(ri.getMethodName())), any(MethodInvocation.class));
			verify(mockTestService).loadFutureUserByUsername(null, futureType);
			verify(mockHttpServletRequest, Mockito.never()).startAsync();
			verifyNoMoreInteractions(mockAsyncContext);
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
			verify(mockHttpInvokerRequestExecutor).executeRequest(eq(serviceUrl(TestService.class)),
					argThat(ri -> "loadFutureUserByUsername".equals(ri.getMethodName())), any(MethodInvocation.class));
			verify(mockHttpServletRequest).startAsync();
			if (futureType == FutureType.RUNNABLE)
				verify(mockAsyncContext).start(any(Runnable.class));
			verify(mockAsyncContext).complete();
			verify(mockTestService).loadFutureUserByUsername("", futureType);
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
		verify(mockHttpInvokerRequestExecutor).executeRequest(eq(serviceUrl(TestService.class)),
				argThat(ri -> "loadListenableFutureUserByUsername".equals(ri.getMethodName())),
				any(MethodInvocation.class));
		verify(mockHttpServletRequest).startAsync();
		verify(mockAsyncContext).complete();
		verify(mockTestService).loadListenableFutureUserByUsername("username");
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
		verify(mockHttpInvokerRequestExecutor).executeRequest(eq(serviceUrl(TestService.class)),
				argThat(ri -> "loadListenableFutureUserDetailsByUsername".equals(ri.getMethodName())),
				any(MethodInvocation.class));
		verify(mockHttpServletRequest).startAsync();
		verify(mockAsyncContext).complete();
		verify(mockTestService).loadListenableFutureUserDetailsByUsername("username");
	}

	@Test
	public void testAttempt() throws Exception {
		final int maxAttempts = 5;
		barServiceClient.setMaxAttempts(maxAttempts);
		doThrow(new Exception("test")).when(mockHttpInvokerRequestExecutor).executeRequest(
				contains(BarService.class.getName()), argThat(ri -> "test".equals(ri.getMethodName())),
				any(MethodInvocation.class));
		boolean error = false;
		try {
			((BarService) (barServiceClient.getObject())).test("");
		} catch (Throwable e) {
			error = true;
		}
		assertTrue(error);
		verify(mockHttpInvokerRequestExecutor, times(maxAttempts)).executeRequest(contains(BarService.class.getName()),
				argThat(ri -> "test".equals(ri.getMethodName())), any(MethodInvocation.class));
		verify(mockBarService, never()).test(eq(""));
	}

	@Test
	public void testSerializationDowngrade() throws Exception {
		String defaultSerizliazer = HttpInvokerSerializers.DEFAULT_SERIALIZER.getSerializationType();
		if (!defaultSerizliazer.equals(serializationType)) {
			final int maxAttempts = 5;
			barServiceClient.setMaxAttempts(maxAttempts);
			doThrow(new SerializationFailedException("test")).when(mockHttpInvokerRequestExecutor).executeRequest(
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
			verify(mockHttpInvokerRequestExecutor).setSerializer(HttpInvokerSerializers.DEFAULT_SERIALIZER);
			verify(mockHttpInvokerRequestExecutor, times(maxAttempts)).executeRequest(
					contains(BarService.class.getName()), argThat(ri -> "test".contentEquals(ri.getMethodName())),
					any(MethodInvocation.class));
			verify(mockBarService, never()).test(eq(""));
			mockHttpInvokerRequestExecutor.setSerializer(HttpInvokerSerializers.ofSerializationType(serializationType));
		}
	}

	@Test
	public void testFallbackWithServiceNotFound() throws Exception {
		final int maxAttempts = 1;
		barServiceClient.setMaxAttempts(maxAttempts);
		doThrow(new ServiceNotFoundException(BarService.class.getName())).when(mockHttpInvokerRequestExecutor)
				.executeRequest(contains(BarService.class.getName()),
						argThat(ri -> "test".contentEquals(ri.getMethodName())), any(MethodInvocation.class));
		assertEquals("fallback:", ((BarService) (barServiceClient.getObject())).test(""));
		verify(mockBarService, never()).test(eq(""));
		verify(fallbackBarService).test(eq(""));
	}

	@Test
	public void testFallbackWithCircuitBreakerOpenException() throws Exception {
		final int maxAttempts = 1;
		barServiceClient.setMaxAttempts(maxAttempts);
		doThrow(new CircuitBreakerOpenException("CircuitBreakerOpen")).when(mockHttpInvokerRequestExecutor)
				.executeRequest(contains(BarService.class.getName()),
						argThat(ri -> "test".contentEquals(ri.getMethodName())), any(MethodInvocation.class));
		assertEquals("fallback:", ((BarService) (barServiceClient.getObject())).test(""));
		verify(mockBarService, never()).test(eq(""));
		verify(fallbackBarService).test(eq(""));
	}

	@Test
	public void testServiceImplementedByFactoryBean() throws Exception {
		fooService.test("test");
		verify(mockHttpInvokerRequestExecutor).executeRequest(eq(serviceUrl(FooService.class)),
				argThat(ri -> "test".equals(ri.getMethodName())), any(MethodInvocation.class));
		assertEquals(HttpServletResponse.SC_OK, mockHttpServletResponse.getStatus());
		verify(mockFooService).test("test");
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