package org.ironrhino.core.remoting.serializer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInvocation;
import org.ironrhino.sample.remoting.TestService;
import org.junit.Test;
import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;

import lombok.Getter;
import lombok.Setter;

public class JavaHttpInvokerSerializerTest {

	protected final HttpInvokerSerializer serializer = HttpInvokerSerializers.ofSerializationType(serializationType());

	protected String serializationType() {
		return "JAVA";
	}

	protected byte[] writeRemoteInvocation(RemoteInvocation ri) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		serializer.writeRemoteInvocation(ri, baos);
		return baos.toByteArray();
	}

	protected RemoteInvocation readRemoteInvocation(Class<?> serviceInterface, byte[] data) throws IOException {
		return serializer.readRemoteInvocation(serviceInterface, new ByteArrayInputStream(data));
	}

	protected byte[] writeRemoteInvocationResult(RemoteInvocation ri, RemoteInvocationResult rir) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		serializer.writeRemoteInvocationResult(ri, rir, baos);
		return baos.toByteArray();
	}

	protected RemoteInvocationResult readRemoteInvocationResult(MethodInvocation mi, byte[] data) throws IOException {
		return serializer.readRemoteInvocationResult(mi, new ByteArrayInputStream(data));
	}

	protected MethodInvocation createMethodInvocation(Method method, Object... arguments) {
		MethodInvocation mi = mock(MethodInvocation.class);
		given(mi.getMethod()).willReturn(method);
		given(mi.getArguments()).willReturn(arguments);
		return mi;
	}

	@Test
	public void testWriteReadRemoteInovcation() throws NoSuchMethodException, SecurityException, IOException {
		Method method = TestService.class.getDeclaredMethod("echo", String.class);
		MethodInvocation mi = createMethodInvocation(method, "test");
		RemoteInvocation ri = serializer.createRemoteInvocation(mi);
		ri = readRemoteInvocation(TestService.class, writeRemoteInvocation(ri));
		assertThat(ri.getMethodName(), is("echo"));
		assertThat(ri.getArguments(), is(new Object[] { "test" }));
		assertThat(ri.getParameterTypes(), is(new Class<?>[] { String.class }));
	}

	@Test
	public void testWriteReadRemoteInvocationResult() throws NoSuchMethodException, SecurityException, IOException {
		Method method = TestService.class.getDeclaredMethod("echo", String.class);
		MethodInvocation mi = createMethodInvocation(method, "test");
		RemoteInvocation ri = serializer.createRemoteInvocation(mi);
		RemoteInvocationResult rir = new RemoteInvocationResult("result");
		byte[] data = writeRemoteInvocationResult(ri, rir);
		rir = readRemoteInvocationResult(mi, data);
		assertThat(rir.getValue(), is("result"));
	}

	@Test
	public void testWriteReadRemoteInvocationResultWithException()
			throws NoSuchMethodException, SecurityException, IOException {
		Method method = TestService.class.getDeclaredMethod("echo", String.class);
		MethodInvocation mi = createMethodInvocation(method, "test");
		RemoteInvocation ri = serializer.createRemoteInvocation(mi);
		RemoteInvocationResult rir = new RemoteInvocationResult(
				new InvocationTargetException(new RuntimeException("error")));
		rir = readRemoteInvocationResult(mi, writeRemoteInvocationResult(ri, rir));
		assertThat(rir.getValue(), is(nullValue()));
		assertThat(rir.hasInvocationTargetException(), is(true));
		assertThat(((InvocationTargetException) rir.getException()).getTargetException().getMessage(), is("error"));
		assertThat(((InvocationTargetException) rir.getException()).getTargetException() instanceof RuntimeException,
				is(true));
	}

	@Test
	public void testWriteReadRemoteInvocationResultWithNull()
			throws NoSuchMethodException, SecurityException, IOException {
		Method method = TestService.class.getDeclaredMethod("echo", String.class);
		MethodInvocation mi = createMethodInvocation(method, "test");
		RemoteInvocation ri = serializer.createRemoteInvocation(mi);
		RemoteInvocationResult rir = new RemoteInvocationResult();
		rir = readRemoteInvocationResult(mi, writeRemoteInvocationResult(ri, rir));
		assertThat(rir.getValue(), is(nullValue()));
	}

	@Test
	public void testNonSerializableObject() throws IOException, NoSuchMethodException {
		Exception e = null;
		try {
			Method method = EchoService.class.getDeclaredMethod("echo", Echo.class);
			MethodInvocation mi = createMethodInvocation(method, new Echo("test"));
			RemoteInvocation ri = serializer.createRemoteInvocation(mi);
			ri = readRemoteInvocation(EchoService.class, writeRemoteInvocation(ri));
			assertThat(ri.getMethodName(), is("echo"));
			assertThat(ri.getParameterTypes(), is(new Class<?>[] { Echo.class }));
			assertThat(((Echo) ri.getArguments()[0]).getEcho(), is("test"));
		} catch (SerializationFailedException error) {
			e = error;
		} catch (RuntimeException error) {
			e = error;
		}
		if (JavaHttpInvokerSerializer.INSTANCE == serializer) {
			assertThat(e, is(notNullValue()));
			assertThat(e instanceof SerializationFailedException, is(true));
		} else if (FstHttpInvokerSerializer.INSTANCE == serializer) {
			assertThat(e, is(notNullValue()));
			assertThat(e instanceof RuntimeException, is(true));
		} else {
			assertThat(e, is(nullValue()));
		}
	}

	@Setter
	@Getter
	static class Echo {

		private String echo;

		public Echo() {
		}

		public Echo(String echo) {
			this.echo = echo;
		}
	}

	static class EchoService {
		public String echo(Echo echo) {
			return echo.getEcho();
		}
	}

}
