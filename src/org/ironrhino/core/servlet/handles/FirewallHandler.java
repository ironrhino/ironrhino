package org.ironrhino.core.servlet.handles;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.servlet.AccessHandler;
import org.ironrhino.core.util.RequestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Integer.MIN_VALUE)
public class FirewallHandler implements AccessHandler {

	public static final String KEY_ALLOWEDADDRPATTERN = "firewallHandler.allowedAddrPattern";

	@Value("${" + KEY_ALLOWEDADDRPATTERN + ":}")
	private String allowedAddrPattern;

	public String getAllowedAddrPattern() {
		return allowedAddrPattern;
	}

	public void setAllowedAddrPattern(String allowedAddrPattern) {
		this.allowedAddrPattern = allowedAddrPattern;
	}

	@Override
	public String getPattern() {
		return null;
	}

	@Override
	public boolean handle(HttpServletRequest request,
			HttpServletResponse response) {
		String addr = RequestUtils.getRemoteAddr(request);
		if (addr.equals("127.0.0.1")) {
			String value = request.getParameter(KEY_ALLOWEDADDRPATTERN);
			if (value != null) {
				if (!"true".equalsIgnoreCase(request.getParameter("readonly")))
					this.allowedAddrPattern = value;
				response.setContentType("text/plain");
				try {
					response.getWriter().write(
							KEY_ALLOWEDADDRPATTERN + "="
									+ this.allowedAddrPattern);
				} catch (IOException e) {
					e.printStackTrace();
				}
				return true;
			}
			return false;
		} else if (!isAllowed(addr, allowedAddrPattern)) {
			try {
				response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return true;
		} else {
			return false;
		}
	}

	private static boolean isAllowed(String addr, String allow) {
		if (StringUtils.isBlank(allow))
			return true;
		String[] arr = allow.split("\\s*,\\s*");
		for (String s : arr) {
			if (org.ironrhino.core.util.StringUtils.matchesWildcard(addr, s))
				return true;
		}
		return false;
	}

}
