package org.ironrhino.core.session;

public interface HttpSessionStore {

	public void save(WrappedHttpSession sesion);

	public void initialize(WrappedHttpSession session);

	public void invalidate(WrappedHttpSession sesion);

}
