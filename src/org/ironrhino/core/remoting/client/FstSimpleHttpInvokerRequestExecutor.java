package org.ironrhino.core.remoting.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.ironrhino.core.remoting.FstHttpInvokerSerializationHelper;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;

public class FstSimpleHttpInvokerRequestExecutor extends SimpleHttpInvokerRequestExecutor {

	protected static final String CONTENT_TYPE_FST_SERIALIZED_OBJECT = "application/x-fst-serialized-object";

	public String getContentType() {
		return CONTENT_TYPE_FST_SERIALIZED_OBJECT;
	}

	@Override
	protected void writeRemoteInvocation(RemoteInvocation invocation, OutputStream os) throws IOException {
		FstHttpInvokerSerializationHelper.writeRemoteInvocation(invocation, os);
	}

	@Override
	protected RemoteInvocationResult readRemoteInvocationResult(InputStream is, String codebaseUrl)
			throws IOException, ClassNotFoundException {
		return FstHttpInvokerSerializationHelper.readRemoteInvocationResult(is);
	}

}