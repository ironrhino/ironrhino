package org.ironrhino.core.remoting.serializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.rmi.RemoteException;

import org.aopalliance.intercept.MethodInvocation;
import org.ironrhino.core.remoting.RemotingContext;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;
import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;

public class FstHttpInvokerSerializer implements HttpInvokerSerializer {

	public static FstHttpInvokerSerializer INSTANCE = new FstHttpInvokerSerializer();

	private FstHttpInvokerSerializer() {

	}

	@Override
	public String getContentType() {
		return RemotingContext.CONTENT_TYPE_FST_SERIALIZED_OBJECT;
	}

	@Override
	public void writeRemoteInvocation(RemoteInvocation invocation, OutputStream os) throws IOException {
		FSTObjectOutput out = new FSTObjectOutput(os);
		try {
			out.writeObject(invocation);
		} finally {
			out.close();
		}
	}

	@Override
	public RemoteInvocation readRemoteInvocation(Class<?> serviceInterface, InputStream is) throws IOException {
		FSTObjectInput in = new FSTObjectInput(is);
		try {
			Object obj = in.readObject();
			if (!(obj instanceof RemoteInvocation)) {
				throw new RemoteException("Deserialized object needs to be assignable to type ["
						+ RemoteInvocation.class.getName() + "]: " + obj);
			}
			return (RemoteInvocation) obj;
		} catch (IOException | ClassNotFoundException e) {
			throw new SerializationFailedException(e.getMessage(), e);
		} finally {
			in.close();
		}
	}

	@Override
	public void writeRemoteInvocationResult(RemoteInvocation invocation, RemoteInvocationResult result, OutputStream os)
			throws IOException {
		FSTObjectOutput out = new FSTObjectOutput(os);
		try {
			out.writeObject(result);
		} finally {
			out.close();
		}
	}

	@Override
	public RemoteInvocationResult readRemoteInvocationResult(MethodInvocation methodInvocation, InputStream is)
			throws IOException {
		FSTObjectInput in = new FSTObjectInput(is);
		try {
			Object obj = in.readObject();
			if (!(obj instanceof RemoteInvocationResult)) {
				throw new RemoteException("Deserialized object needs to be assignable to type ["
						+ RemoteInvocationResult.class.getName() + "]: " + obj);
			}
			return (RemoteInvocationResult) obj;
		} catch (IOException | ClassNotFoundException e) {
			throw new SerializationFailedException(e.getMessage(), e);
		} finally {
			in.close();
		}
	}
}
