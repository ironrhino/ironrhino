package org.ironrhino.core.session;

public interface SessionCompressor<T> {

	boolean supportsKey(String key);

	String compress(T value) throws Exception;

	T uncompress(String string) throws Exception;

}
