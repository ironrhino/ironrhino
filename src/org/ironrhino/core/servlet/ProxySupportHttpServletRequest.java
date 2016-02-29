package org.ironrhino.core.servlet;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.lang3.StringUtils;

public class ProxySupportHttpServletRequest extends HttpServletRequestWrapper {

	// proxy_set_header X-Real-IP $remote_addr;
	public static final String HEADER_NAME_X_REAL_IP = "X-Real-IP";

	// proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
	public static final String HEADER_NAME_X_FORWARDED_FOR = "X-Forwarded-For";

	// proxy_set_header X-Forwarded-For $scheme;
	public static final String HEADER_NAME_X_FORWARDED_PROTO = "X-Forwarded-Proto";

	// proxy_set_header X-Forwarded-For $server_port;
	public static final String HEADER_NAME_X_FORWARDED_PORT = "X-Forwarded-Port";

	// proxy_set_header X-Url-Scheme $scheme;
	public static final String HEADER_NAME_X_URL_SCHEME = "X-Url-Scheme";

	// proxy_set_header X-Client-Certificate $ssl_client_cert;
	public static final String HEADER_NAME_X_CLIENT_CERTIFICATE = "X-Client-Certificate";

	public ProxySupportHttpServletRequest(HttpServletRequest request) {
		super(request);
		String certificate = request.getHeader(HEADER_NAME_X_CLIENT_CERTIFICATE);
		if (StringUtils.isNotBlank(certificate)) {
			// nginx replaced line separator with whitespace and tab
			String certificateContent = certificate.replaceAll("\\s{2,}", System.lineSeparator()).replaceAll("\\t+",
					System.lineSeparator());
			try {
				X509Certificate x509Certificate = (X509Certificate) CertificateFactory.getInstance("X.509")
						.generateCertificate(new ByteArrayInputStream(certificateContent.getBytes("ISO-8859-11")));
				request.setAttribute("javax.servlet.request.X509Certificate",
						new X509Certificate[] { x509Certificate });
			} catch (CertificateException | UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
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
			scheme = getHeader(HEADER_NAME_X_FORWARDED_PROTO);
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
		String port = getHeader(HEADER_NAME_X_FORWARDED_PORT);
		if (StringUtils.isNumeric(port))
			return Integer.valueOf(port);
		String host = getHeader("Host");
		if (StringUtils.isNotBlank(host)) {
			int index = host.lastIndexOf(':');
			if (index > 0) {
				port = host.substring(index + 1);
				if (StringUtils.isNumeric(port))
					return Integer.valueOf(port);
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
