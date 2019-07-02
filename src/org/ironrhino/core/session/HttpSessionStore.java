package org.ironrhino.core.session;

public interface HttpSessionStore {

	void save(WrappedHttpSession session);

	void initialize(WrappedHttpSession session);

	void invalidate(WrappedHttpSession session);

}
