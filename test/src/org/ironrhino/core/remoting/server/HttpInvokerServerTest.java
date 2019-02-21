package org.ironrhino.core.remoting.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.aopalliance.intercept.MethodInvocation;
import org.ironrhino.core.remoting.BaseHttpInvokerTest;
import org.ironrhino.core.remoting.RemotingContext;
import org.ironrhino.core.remoting.serializer.HttpInvokerSerializers;
import org.ironrhino.sample.remoting.FooService;
import org.ironrhino.sample.remoting.TestService;
import org.ironrhino.sample.remoting.TestService.FutureType;
import org.ironrhino.security.domain.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.concurrent.ListenableFuture;

@RunWith(SpringRunner.class)
public class HttpInvokerServerTest extends BaseHttpInvokerTest {

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
		request.setRequestURI("/dummyService");
		MockHttpServletResponse response = new MockHttpServletResponse();
		httpInvokerServer.handleRequest(request, response);
		assertEquals(HttpServletResponse.SC_NOT_FOUND, response.getStatus());
	}

	@Test
	public void testSerializationFailed() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI(TestService.class.getName());
		request.addHeader(HttpHeaders.CONTENT_TYPE, serializer.getContentType());

		MockHttpServletResponse response = new MockHttpServletResponse();
		httpInvokerServer.handleRequest(request, response);
		if (HttpInvokerSerializers.DEFAULT_SERIALIZER.getSerializationType()
				.equals(serializer.getSerializationType())) {
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
			reset();
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
			reset();
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
			reset();
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
	public void testServiceImplementedByFactoryBean() throws Exception {
		fooService.test("test");
		verify(mockHttpInvokerRequestExecutor).executeRequest(eq(serviceUrl(FooService.class)),
				argThat(ri -> "test".equals(ri.getMethodName())), any(MethodInvocation.class));
		assertEquals(HttpServletResponse.SC_OK, mockHttpServletResponse.getStatus());
		verify(mockFooService).test("test");

		RemoteInvocation remoteInvocation = new RemoteInvocation();
		remoteInvocation.setMethodName("test");
		remoteInvocation.setParameterTypes(new Class[] { String.class });
		remoteInvocation.setArguments(new Object[] { "hello" });
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		serializer.writeRemoteInvocation(remoteInvocation, baos);
		baos.close();

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI(serviceUri(FooService.class));
		request.addHeader(HttpHeaders.CONTENT_TYPE, serializer.getContentType());
		request.setContent(baos.toByteArray());

		MockHttpServletResponse response = new MockHttpServletResponse();
		httpInvokerServer.handleRequest(request, response);
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
	}

}