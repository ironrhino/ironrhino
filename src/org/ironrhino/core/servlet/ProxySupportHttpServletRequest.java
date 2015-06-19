package org.ironrhino.core.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.lang3.StringUtils;

public class ProxySupportHttpServletRequest extends HttpServletRequestWrapper {

	public static final String HEADER_NAME_X_REAL_IP = "X-Real-IP";

	public static final String HEADER_NAME_X_FORWARDED_FOR = "X-Forwarded-For";

	public static final String HEADER_NAME_X_URL_SCHEME = "X-Url-Scheme";

	public ProxySupportHttpServletRequest(HttpServletRequest request) {
		super(request);
	}

	@Override
	public String getRemoteAddr() {
		String addr = getHeader(HEADER_NAME_X_REAL_IP);
		if (StringUtils.isBlank(addr)) {
			addr = getHeader(HEADER_NAME_X_FORWARDED_FOR);
			int index = 0;
			if (StringUtils.isNotBlank(addr) && (index = addr.indexOf(',')) > 0)
				addr = addr.substring(0, index);
		}
		addr = StringUtils.isNotBlank(addr) ? addr : super.getRemoteAddr();
		return addr;
	}

	@Override
	public String getScheme() {
		String scheme = getHeader(HEADER_NAME_X_URL_SCHEME);
		if (StringUtils.isBlank(scheme))
			scheme = super.getScheme();
		return scheme;
	}

	@Override
	public String getServerName() {
		String host = getHeader("Host");
		if (StringUtils.isNotBlank(host)) {
			int index = host.lastIndexOf(':');
			if (index > 0) {
				String p = host.substring(index + 1);
				if (StringUtils.isNumeric(p))
					return host.substring(0, index);
				else
					return host;
			} else {
				return host;
			}
		}
		return super.getServerName();
	}

	@Override
	public int getServerPort() {
		String host = getHeader("Host");
		if (StringUtils.isNotBlank(host)) {
			int index = host.lastIndexOf(':');
			if (index > 0) {
				String p = host.substring(index + 1);
				if (StringUtils.isNumeric(p))
					return Integer.valueOf(p);
				else
					return isSecure() ? 443 : 80;
			} else {
				return isSecure() ? 443 : 80;
			}
		}
		return super.getServerPort();
	}

	@Override
	public StringBuffer getRequestURL() {
		StringBuffer sb = new StringBuffer(getScheme());
		sb.append("://");
		String host = getHeader("Host");
		if (StringUtils.isNotBlank(host)) {
			sb.append(host);
		} else {
			sb.append(getServerName());
			int port = getServerPort();
			if (!(!isSecure() && port == 80 || isSecure() && port == 443))
				sb.append(":").append(port);
		}
		sb.append(getRequestURI());
		return sb;
	}

	@Override
	public boolean isSecure() {
		return getScheme().equals("https") || super.isSecure();
	}

}
