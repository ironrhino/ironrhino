package org.ironrhino.core.session;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface HttpSessionFilterHook {

	public boolean beforeFilterChain(HttpServletRequest request, HttpServletResponse response);

	public void afterFilterChain(HttpServletRequest request, HttpServletResponse response);

}
