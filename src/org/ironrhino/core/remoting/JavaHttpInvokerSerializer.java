package org.ironrhino.core.remoting;

import java.io.IOException;
import java.io.InputStream;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.OutputStream;

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
		try (ObjectOutputStream oos = new ObjectOutputStream(os)) {
			oos.writeObject(invocation);
		} catch (NotSerializableException e) {
			throw new SerializationFailedException(e.getMessage(), e);
		}
	}

	@Override
	public RemoteInvocation readRemoteInvocation(InputStream is) throws IOException {
		try (ObjectInputStream ois = new ObjectInputStream(is)) {
			return (RemoteInvocation) ois.readObject();
		} catch (ObjectStreamException | ClassNotFoundException e) {
			throw new SerializationFailedException(e.getMessage(), e);
		}
	}

	@Override
	public void writeRemoteInvocationResult(RemoteInvocation invocation, RemoteInvocationResult result, OutputStream os)
			throws IOException {
		try (ObjectOutputStream oos = new ObjectOutputStream(os)) {
			oos.writeObject(result);
		} catch (NotSerializableException e) {
			throw new SerializationFailedException(e.getMessage(), e);
		}
	}

	@Override
	public RemoteInvocationResult readRemoteInvocationResult(InputStream is) throws IOException {
		try (ObjectInputStream ois = new ObjectInputStream(is)) {
			return (RemoteInvocationResult) ois.readObject();
		} catch (ObjectStreamException | ClassNotFoundException e) {
			throw new SerializationFailedException(e.getMessage(), e);
		}
	}
}
