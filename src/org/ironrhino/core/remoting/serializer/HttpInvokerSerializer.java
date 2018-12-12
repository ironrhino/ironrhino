package org.ironrhino.core.remoting.serializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

import javax.servlet.http.HttpServletResponse;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;

public interface HttpInvokerSerializer {

	public default String getSerializationType() {
		String name = getClass().getSimpleName();
		String suffix = HttpInvokerSerializer.class.getSimpleName();
		if (name.endsWith(suffix))
			name = name.substring(0, name.length() - suffix.length());
		return name.toUpperCase(Locale.ROOT);
	}

	public String getContentType();

	public default RemoteInvocation createRemoteInvocation(MethodInvocation methodInvocation) {
		return new RemoteInvocation(methodInvocation);
	}

	public void writeRemoteInvocation(RemoteInvocation invocation, OutputStream os) throws IOException;

	public RemoteInvocation readRemoteInvocation(Class<?> serviceInterface, InputStream is) throws IOException;

	public void writeRemoteInvocationResult(RemoteInvocation invocation, RemoteInvocationResult result, OutputStream os)
			throws IOException;

	public RemoteInvocationResult readRemoteInvocationResult(MethodInvocation methodInvocation, InputStream is)
			throws IOException;

	public default boolean handleException(Exception ex, HttpServletResponse response) throws IOException {
		return false;
	}

}
