package org.ironrhino.core.remoting;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.rmi.RemoteException;

import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;
import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;

public class FstHttpInvokerSerializationHelper {

	public static RemoteInvocation readRemoteInvocation(InputStream is) throws IOException, ClassNotFoundException {
		FSTObjectInput in = new FSTObjectInput(is);
		try {
			Object obj = in.readObject();
			if (!(obj instanceof RemoteInvocation)) {
				throw new RemoteException("Deserialized object needs to be assignable to type ["
						+ RemoteInvocation.class.getName() + "]: " + obj);
			}
			return (RemoteInvocation) obj;
		} catch (IOException e) {
			throw new SerializationFailedException(e.getMessage(), e);
		} finally {
			in.close();
		}
	}

	public static void writeRemoteInvocationResult(RemoteInvocationResult result, OutputStream os) throws IOException {
		FSTObjectOutput out = new FSTObjectOutput(os);
		try {
			out.writeObject(result);
		} finally {
			out.close();
		}
	}

	public static void writeRemoteInvocation(RemoteInvocation invocation, OutputStream os) throws IOException {
		FSTObjectOutput out = new FSTObjectOutput(os);
		try {
			out.writeObject(invocation);
		} finally {
			out.close();
		}
	}

	public static RemoteInvocationResult readRemoteInvocationResult(InputStream is)
			throws IOException, ClassNotFoundException {
		FSTObjectInput in = new FSTObjectInput(is);
		try {
			Object obj = in.readObject();
			if (!(obj instanceof RemoteInvocationResult)) {
				throw new RemoteException("Deserialized object needs to be assignable to type ["
						+ RemoteInvocationResult.class.getName() + "]: " + obj);
			}
			return (RemoteInvocationResult) obj;
		} catch (IOException e) {
			throw new SerializationFailedException(e.getMessage(), e);
		} finally {
			in.close();
		}
	}
}
