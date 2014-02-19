package org.ironrhino.core.servlet.handles;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.servlet.AccessHandler;
import org.ironrhino.core.util.RequestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Integer.MIN_VALUE + 1)
public class CorsHandler implements AccessHandler {

	@Value("${cors.openForSameOrigin:true}")
	private boolean openForSameOrigin;

	@Value("${cors.xFrameOptions:SAMEORIGIN}")
	private String xFrameOptions = "SAMEORIGIN";

	@Override
	public String getPattern() {
		return null;
	}

	@Override
	public boolean handle(HttpServletRequest request,
			HttpServletResponse response) {
		response.setHeader("X-Frame-Options", xFrameOptions);
		String origin = request.getHeader("Origin");
		if (StringUtils.isNotBlank(origin)) {
			if (!("Upgrade".equalsIgnoreCase(request.getHeader("Connection")) && "WebSocket"
					.equalsIgnoreCase(request.getHeader("Upgrade")))) {
				String url = request.getRequestURL().toString();
				if ((openForSameOrigin || RequestUtils
						.isSameOrigin(url, origin)) && !url.startsWith(origin)) {
					response.setHeader("Access-Control-Allow-Origin", origin);
					response.setHeader("Access-Control-Allow-Credentials",
							"true");
					String requestMethod = request
							.getHeader("Access-Control-Request-Method");
					String requestHeaders = request
							.getHeader("Access-Control-Request-Headers");
					String method = request.getMethod();
					if (method.equalsIgnoreCase("OPTIONS")
							&& (requestMethod != null || requestHeaders != null)) {
						// preflighted request
						if (StringUtils.isNotBlank(requestMethod))
							response.setHeader("Access-Control-Allow-Methods",
									requestMethod);
						if (StringUtils.isNotBlank(requestHeaders))
							response.setHeader("Access-Control-Allow-Headers",
									requestHeaders);
						response.setHeader("Access-Control-Max-Age", "36000");
						return true;
					}
				}
			}
		}
		return false;
	}

}
