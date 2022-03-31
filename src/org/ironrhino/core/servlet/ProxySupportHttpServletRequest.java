package org.ironrhino.core.servlet;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

public class ProxySupportHttpServletRequest extends HttpServletRequestWrapper {

	public static final String SYSTEM_PROPERTY_PROXY_REQUEST_DISABLED = "proxy.request.disabled";
	public static final String SYSTEM_PROPERTY_PROXY_TRUSTED_ADDRESS = "proxy.trusted.address";

	// proxy_set_header X-Real-IP $remote_addr;
	public static final String HEADER_NAME_X_REAL_IP = "X-Real-IP";

	// proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
	public static final String HEADER_NAME_X_FORWARDED_FOR = "X-Forwarded-For";

	// proxy_set_header X-Forwarded-Proto $scheme;
	public static final String HEADER_NAME_X_FORWARDED_PROTO = "X-Forwarded-Proto";

	// proxy_set_header X-Forwarded-Host $server_name;
	public static final String HEADER_NAME_X_FORWARDED_HOST = "X-Forwarded-Host";

	// proxy_set_header X-Forwarded-Port $server_port;
	public static final String HEADER_NAME_X_FORWARDED_PORT = "X-Forwarded-Port";

	public static final String HEADER_NAME_X_FORWARDED_PREFIX = "X-Forwarded-Prefix";

	// proxy_set_header X-Url-Scheme $scheme;
	public static final String HEADER_NAME_X_URL_SCHEME = "X-Url-Scheme";

	// proxy_set_header X-Client-Certificate $ssl_client_cert;
	public static final String HEADER_NAME_X_CLIENT_CERTIFICATE = "X-Client-Certificate";
	public static final String HEADER_NAME_X_CLIENT_CERT = "X-Client-Cert";

	public static final String REQUEST_ATTRIBUTE_PROXY_ADDR = "X-Proxy-Addr";

	private static final String trustedAddress = System.getProperty(SYSTEM_PROPERTY_PROXY_TRUSTED_ADDRESS);

	private String remoteAddr;

	public ProxySupportHttpServletRequest(HttpServletRequest request) {
		super(request);
		String certificate = request.getHeader(HEADER_NAME_X_CLIENT_CERTIFICATE);
		if (certificate == null)
			certificate = request.getHeader(HEADER_NAME_X_CLIENT_CERT);
		if (StringUtils.isNotBlank(certificate)) {
			// nginx replaced line separator with whitespace and tab
			String certificateContent = certificate.replaceAll("\\s{2,}", System.lineSeparator()).replaceAll("\\t+",
					System.lineSeparator());
			try {
				X509Certificate x509Certificate = (X509Certificate) CertificateFactory.getInstance("X.509")
						.generateCertificate(
								new ByteArrayInputStream(certificateContent.getBytes(StandardCharsets.ISO_8859_1)));
				request.setAttribute("javax.servlet.request.X509Certificate",
						new X509Certificate[] { x509Certificate });
			} catch (CertificateException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public String getRemoteAddr() {
		if (remoteAddr == null) {
			String superRemoteAddr = super.getRemoteAddr();
			String realIp = getHeader(HEADER_NAME_X_REAL_IP);
			String forwardedFor = getHeader(HEADER_NAME_X_FORWARDED_FOR);
			String temp = null;
			if (StringUtils.isNotBlank(realIp) && StringUtils.isNotBlank(forwardedFor)
					&& !realIp.equals(forwardedFor)) {
				String[] arr1 = forwardedFor.split("\\s*,\\s*");
				boolean trustedForward = arr1[arr1.length - 1].equals(realIp) && isTrustedProxy(superRemoteAddr);
				if (trustedForward) {
					temp = forwardedFor;
					int index = 0;
					if ((index = temp.indexOf(',')) > 0)
						temp = temp.substring(0, index).trim();
				} else {
					temp = realIp;
				}
			} else if (StringUtils.isNotBlank(realIp)) {
				temp = realIp;
			} else if (StringUtils.isNotBlank(forwardedFor)) {
				temp = forwardedFor;
				int index = 0;
				if ((index = temp.indexOf(',')) > 0)
					temp = temp.substring(0, index).trim();
			}
			if (StringUtils.isNotBlank(temp)) {
				setAttribute(REQUEST_ATTRIBUTE_PROXY_ADDR, super.getRemoteAddr());
			} else {
				temp = superRemoteAddr;
			}
			remoteAddr = temp;
		}
		return remoteAddr;
	}

	private static boolean isTrustedProxy(String remoteAddr) {
		if (trustedAddress != null) {
			return Arrays.asList(trustedAddress.split(",")).contains(remoteAddr);
		} else {
			for (String subnet : "172.16.0.0/12,192.168.0.0/16,10.0.0.0/8,169.254.0.0/16,127.0.0.1/8,FEC0::/7,::1"
					.split(","))
				if (new IpAddressMatcher(subnet).matches(remoteAddr))
					return true;
			return false;
		}
	}

	@Override
	public String getScheme() {
		String scheme = getHeader(HEADER_NAME_X_URL_SCHEME);
		if (StringUtils.isBlank(scheme))
			scheme = getHeader(HEADER_NAME_X_FORWARDED_PROTO);
		if (StringUtils.isBlank(scheme))
			scheme = super.getScheme();
		return scheme;
	}

	@Override
	public String getServerName() {
		String host = getHeader(HEADER_NAME_X_FORWARDED_HOST);
		if (StringUtils.isNotBlank(host))
			return host;
		host = getHeader("Host");
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
		String port = getHeader(HEADER_NAME_X_FORWARDED_PORT);
		if (StringUtils.isNumeric(port))
			return Integer.parseInt(port);
		String host = getHeader("Host");
		if (StringUtils.isNotBlank(host)) {
			int index = host.lastIndexOf(':');
			if (index > 0) {
				port = host.substring(index + 1);
				if (StringUtils.isNumeric(port))
					return Integer.parseInt(port);
				else
					return isSecure() ? 443 : 80;
			} else {
				return isSecure() ? 443 : 80;
			}
		}
		return super.getServerPort();
	}

	@Override
	public String getContextPath() {
		String prefix = getForwardedPrefix();
		return prefix != null ? prefix : super.getContextPath();
	}

	@Override
	public String getRequestURI() {
		String prefix = getForwardedPrefix();
		if (prefix != null) {
			String contextPath = super.getContextPath();
			String requestURI = super.getRequestURI();
			return prefix + requestURI.substring(contextPath.length());
		} else {
			return super.getRequestURI();
		}
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

	private String getForwardedPrefix() {
		String prefix = getHeader(HEADER_NAME_X_FORWARDED_PREFIX);
		if (prefix != null && prefix.endsWith("/"))
			prefix = prefix.substring(0, prefix.length() - 1);
		return prefix;
	}

	public static boolean isProxyable(HttpServletRequest request) {
		return !"true".equals(System.getProperty(SYSTEM_PROPERTY_PROXY_REQUEST_DISABLED))
				&& (request.getHeader(ProxySupportHttpServletRequest.HEADER_NAME_X_REAL_IP) != null
						|| request.getHeader(ProxySupportHttpServletRequest.HEADER_NAME_X_FORWARDED_FOR) != null
						|| request.getHeader(ProxySupportHttpServletRequest.HEADER_NAME_X_FORWARDED_PREFIX) != null);
	}

}
