package org.ironrhino.core.session;

public interface HttpSessionFilterHook {

	public void beforeDoFilter();

	public void afterDoFilter();

}
