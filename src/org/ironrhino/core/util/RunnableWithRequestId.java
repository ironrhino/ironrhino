package org.ironrhino.core.util;

public class RunnableWithRequestId implements Runnable {

	private final Runnable delegate;

	public RunnableWithRequestId(Runnable delegate) {
		this.delegate = delegate;
	}

	@Override
	public void run() {
		CodecUtils.putRequestIdIfAbsent();
		delegate.run();
	}

}
