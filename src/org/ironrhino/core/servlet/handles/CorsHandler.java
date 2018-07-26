package org.ironrhino.core.servlet.handles;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.servlet.AccessHandler;
import org.ironrhino.core.util.RequestUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@Getter
@Setter
@ConfigurationProperties(prefix = "cors")
public class CorsHandler extends AccessHandler {

	private boolean openForAllOrigin = false;

	private boolean openForSameOrigin = true;

	private String xFrameOptions = "SAMEORIGIN";

	@Override
	public boolean handle(HttpServletRequest request, HttpServletResponse response) {
		response.setHeader("X-Powered-By", "Ironrhino");
		response.setHeader("X-Frame-Options", xFrameOptions);
		String origin = request.getHeader("Origin");
		if (StringUtils.isNotBlank(origin)) {
			if (!("Upgrade".equalsIgnoreCase(request.getHeader("Connection"))
					&& "WebSocket".equalsIgnoreCase(request.getHeader("Upgrade")))) {
				String url = request.getRequestURL().toString();
				if (openForAllOrigin
						|| openForSameOrigin && RequestUtils.isSameOrigin(url, origin) && !url.startsWith(origin)) {
					response.setHeader("Access-Control-Allow-Origin", origin);
					if (!openForAllOrigin)
						response.setHeader("Access-Control-Allow-Credentials", "true");
					String requestMethod = request.getHeader("Access-Control-Request-Method");
					String requestHeaders = request.getHeader("Access-Control-Request-Headers");
					String method = request.getMethod();
					if (method.equalsIgnoreCase("OPTIONS") && (requestMethod != null || requestHeaders != null)) {
						// preflighted request
						if (StringUtils.isNotBlank(requestMethod))
							response.setHeader("Access-Control-Allow-Methods", requestMethod);
						if (StringUtils.isNotBlank(requestHeaders))
							response.setHeader("Access-Control-Allow-Headers", requestHeaders);
						response.setHeader("Access-Control-Max-Age", "36000");
						return true;
					}
				}
			}
		}
		return false;
	}

}
