package org.ironrhino.core.util;

public interface ThrowableRunnable<E extends Throwable> {

	void run() throws E;

}