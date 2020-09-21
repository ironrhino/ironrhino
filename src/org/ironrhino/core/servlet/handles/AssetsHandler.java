package org.ironrhino.core.servlet.handles;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ironrhino.core.servlet.AccessHandler;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 3)
public class AssetsHandler extends AccessHandler {

	@Override
	public String getPattern() {
		return "/assets/*/ironrhino-min.*";
	}

	@Override
	public boolean handle(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		String uri = request.getRequestURI();
		if (request.getServletContext().getResource(uri) == null) {
			request.getRequestDispatcher(uri.replace("-min", "")).forward(request, response);
			return true;
		} else {
			return false;
		}
	}

}
