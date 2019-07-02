package org.ironrhino.core.session;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface HttpSessionFilterHook {

	boolean beforeFilterChain(HttpServletRequest request, HttpServletResponse response);

	void afterFilterChain(HttpServletRequest request, HttpServletResponse response);

}
