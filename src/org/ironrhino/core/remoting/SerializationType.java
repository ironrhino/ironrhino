package org.ironrhino.core.remoting;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import org.springframework.remoting.httpinvoker.AbstractHttpInvokerRequestExecutor;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;

public enum SerializationType {

	JAVA(AbstractHttpInvokerRequestExecutor.CONTENT_TYPE_SERIALIZED_OBJECT) {
		@Override
		public void writeRemoteInvocation(RemoteInvocation invocation, OutputStream os) throws IOException {
			ObjectOutputStream oos = new ObjectOutputStream(os);
			try {
				oos.writeObject(invocation);
			} finally {
				oos.close();
			}
		}

		@Override
		public RemoteInvocation readRemoteInvocation(InputStream is) throws IOException, ClassNotFoundException {
			ObjectInputStream ois = new ObjectInputStream(is);
			try {
				return (RemoteInvocation) ois.readObject();
			} finally {
				ois.close();
			}
		}

		@Override
		public void writeRemoteInvocationResult(RemoteInvocation invocation, RemoteInvocationResult result,
				OutputStream os) throws IOException {
			ObjectOutputStream oos = new ObjectOutputStream(os);
			try {
				oos.writeObject(result);
			} finally {
				oos.close();
			}
		}

		@Override
		public RemoteInvocationResult readRemoteInvocationResult(InputStream is)
				throws IOException, ClassNotFoundException {
			ObjectInputStream ois = new ObjectInputStream(is);
			try {
				return (RemoteInvocationResult) ois.readObject();
			} finally {
				ois.close();
			}
		}
	},
	FST("application/x-fst-serialized-object") {
		@Override
		public void writeRemoteInvocation(RemoteInvocation invocation, OutputStream os) throws IOException {
			FstHttpInvokerSerializationHelper.writeRemoteInvocation(invocation, os);
		}

		@Override
		public RemoteInvocation readRemoteInvocation(InputStream is) throws IOException, ClassNotFoundException {
			return FstHttpInvokerSerializationHelper.readRemoteInvocation(is);
		}

		@Override
		public void writeRemoteInvocationResult(RemoteInvocation invocation, RemoteInvocationResult result,
				OutputStream os) throws IOException {
			FstHttpInvokerSerializationHelper.writeRemoteInvocationResult(invocation, result, os);
		}

		@Override
		public RemoteInvocationResult readRemoteInvocationResult(InputStream is)
				throws IOException, ClassNotFoundException {
			return FstHttpInvokerSerializationHelper.readRemoteInvocationResult(is);
		}
	},
	JSON("application/x-json-serialized-object") {
		@Override
		public void writeRemoteInvocation(RemoteInvocation invocation, OutputStream os) throws IOException {
			JsonHttpInvokerSerializationHelper.writeRemoteInvocation(invocation, os);
		}

		@Override
		public RemoteInvocation readRemoteInvocation(InputStream is) throws IOException, ClassNotFoundException {
			return JsonHttpInvokerSerializationHelper.readRemoteInvocation(is);
		}

		@Override
		public void writeRemoteInvocationResult(RemoteInvocation invocation, RemoteInvocationResult result,
				OutputStream os) throws IOException {
			JsonHttpInvokerSerializationHelper.writeRemoteInvocationResult(invocation, result, os);
		}

		@Override
		public RemoteInvocationResult readRemoteInvocationResult(InputStream is)
				throws IOException, ClassNotFoundException {
			return JsonHttpInvokerSerializationHelper.readRemoteInvocationResult(is);
		}
	};

	private String contentType;

	private SerializationType(String contentType) {
		this.contentType = contentType;
	}

	public String getContentType() {
		return contentType;
	}

	public abstract void writeRemoteInvocation(RemoteInvocation invocation, OutputStream os) throws IOException;

	public abstract RemoteInvocation readRemoteInvocation(InputStream is) throws IOException, ClassNotFoundException;

	public abstract void writeRemoteInvocationResult(RemoteInvocation invocation, RemoteInvocationResult result,
			OutputStream os) throws IOException;

	public abstract RemoteInvocationResult readRemoteInvocationResult(InputStream is)
			throws IOException, ClassNotFoundException;

	public static SerializationType parse(String contentType) {
		for (SerializationType type : values())
			if (type.getContentType().equalsIgnoreCase(contentType))
				return type;
		return JAVA;
	}

}