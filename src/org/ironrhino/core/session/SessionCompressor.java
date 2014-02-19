package org.ironrhino.core.session;

public interface SessionCompressor<T> {

	public boolean supportsKey(String key);

	public String compress(T value) throws Exception;

	public T uncompress(String string) throws Exception;

}
