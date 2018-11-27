package org.ironrhino.core.remoting.client;

import java.io.ByteArrayOutputStream;

import org.aopalliance.intercept.MethodInvocation;
import org.ironrhino.core.remoting.serializer.HttpInvokerSerializer;
import org.ironrhino.core.remoting.serializer.HttpInvokerSerializers;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class HttpInvokerRequestExecutor {

	private static final int SERIALIZED_INVOCATION_BYTE_ARRAY_INITIAL_SIZE = 1024;

	protected static final String HTTP_METHOD_POST = "POST";

	protected static final String HTTP_HEADER_ACCEPT_ENCODING = "Accept-Encoding";

	protected static final String HTTP_HEADER_CONTENT_ENCODING = "Content-Encoding";

	protected static final String HTTP_HEADER_CONTENT_TYPE = "Content-Type";

	protected static final String HTTP_HEADER_CONTENT_LENGTH = "Content-Length";

	protected static final String ENCODING_GZIP = "gzip";

	private volatile HttpInvokerSerializer serializer = HttpInvokerSerializers.DEFAULT_SERIALIZER;

	private boolean acceptGzipEncoding = true;

	private int connectTimeout = -1;

	private int readTimeout = -1;

	public RemoteInvocationResult executeRequest(String serviceUrl, RemoteInvocation invocation,
			MethodInvocation methodInvocation) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(SERIALIZED_INVOCATION_BYTE_ARRAY_INITIAL_SIZE);
		serializer.writeRemoteInvocation(invocation, baos);
		return doExecuteRequest(serviceUrl, methodInvocation, baos);
	}

	protected abstract RemoteInvocationResult doExecuteRequest(String serviceUrl, MethodInvocation methodInvocation,
			ByteArrayOutputStream baos) throws Exception;

}
