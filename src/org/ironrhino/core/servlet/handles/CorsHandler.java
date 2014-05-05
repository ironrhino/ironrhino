package org.ironrhino.core.servlet.handles;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.servlet.AccessHandler;
import org.ironrhino.core.util.RequestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 使用CORS，发送一些带凭据的跨域请求. 一般默认情况下都是不启用带有凭据的跨域，
 * 因此如果想要启用的话，只需将XMLHttpRequest的withCredentials属性设置为True.
 */
@Component
@Order(Integer.MIN_VALUE + 1)
public class CorsHandler implements AccessHandler {
    // 设置相同的请求源开关
	@Value("${cors.openForSameOrigin:true}")
	private boolean openForSameOrigin;
	// 设置相同的请求源标识
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
