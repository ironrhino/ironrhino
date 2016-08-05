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
public class FaviconHandler extends AccessHandler {

	@Override
	public String getPattern() {
		return "/favicon.ico";
	}

	@Override
	public boolean handle(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		request.getRequestDispatcher("/assets/images/favicon.ico").forward(request, response);
		//response.sendRedirect(request.getContextPath() + "/assets/images/favicon.ico");
		return true;
	}

}
