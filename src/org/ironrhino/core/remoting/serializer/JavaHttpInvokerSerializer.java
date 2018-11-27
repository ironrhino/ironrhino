package org.ironrhino.core.remoting.serializer;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.OutputStream;

import org.aopalliance.intercept.MethodInvocation;
import org.ironrhino.core.remoting.RemotingContext;
import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;

public class JavaHttpInvokerSerializer implements HttpInvokerSerializer {

	public static JavaHttpInvokerSerializer INSTANCE = new JavaHttpInvokerSerializer();

	private JavaHttpInvokerSerializer() {

	}

	@Override
	public String getContentType() {
		return RemotingContext.CONTENT_TYPE_JAVA_SERIALIZED_OBJECT;
	}

	@Override
	public void writeRemoteInvocation(RemoteInvocation invocation, OutputStream os) throws IOException {
		try (ObjectOutputStream oos = new ObjectOutputStream(new FlushGuardedOutputStream(os))) {
			oos.writeObject(invocation);
		} catch (NotSerializableException e) {
			throw new SerializationFailedException(e.getMessage(), e);
		}
	}

	@Override
	public RemoteInvocation readRemoteInvocation(Class<?> serviceInterface, InputStream is) throws IOException {
		try (ObjectInputStream ois = new ObjectInputStream(is)) {
			return (RemoteInvocation) ois.readObject();
		} catch (ObjectStreamException | ClassNotFoundException e) {
			throw new SerializationFailedException(e.getMessage(), e);
		}
	}

	@Override
	public void writeRemoteInvocationResult(RemoteInvocation invocation, RemoteInvocationResult result, OutputStream os)
			throws IOException {
		try (ObjectOutputStream oos = new ObjectOutputStream(new FlushGuardedOutputStream(os))) {
			oos.writeObject(result);
		} catch (NotSerializableException e) {
			throw new SerializationFailedException(e.getMessage(), e);
		}
	}

	@Override
	public RemoteInvocationResult readRemoteInvocationResult(MethodInvocation methodInvocation, InputStream is)
			throws IOException {
		try (ObjectInputStream ois = new ObjectInputStream(is)) {
			return (RemoteInvocationResult) ois.readObject();
		} catch (ObjectStreamException | ClassNotFoundException e) {
			throw new SerializationFailedException(e.getMessage(), e);
		}
	}

	/**
	 * Decorate an {@code OutputStream} to guard against {@code flush()} calls,
	 * which are turned into no-ops.
	 * <p>
	 * Because {@link ObjectOutputStream#close()} will in fact flush/drain the
	 * underlying stream twice, this {@link FilterOutputStream} will guard against
	 * individual flush calls. Multiple flush calls can lead to performance issues,
	 * since writes aren't gathered as they should be.
	 * 
	 * @see <a href="https://jira.spring.io/browse/SPR-14040">SPR-14040</a>
	 */
	private static class FlushGuardedOutputStream extends FilterOutputStream {

		public FlushGuardedOutputStream(OutputStream out) {
			super(out);
		}

		@Override
		public void flush() throws IOException {
			// Do nothing on flush
		}
	}
}
