package org.ironrhino.core.servlet.handles;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ironrhino.core.servlet.AccessHandler;
import org.ironrhino.core.util.AppInfo;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Integer.MIN_VALUE + 2)
public class PingHandler extends AccessHandler {

	@Override
	public String getPattern() {
		return "/_ping";
	}

	@Override
	public boolean handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("text/plain");
		response.getWriter().write(AppInfo.getInstanceId());
		return true;
	}

}
