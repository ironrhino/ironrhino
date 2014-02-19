package org.ironrhino.core.spring.security;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.RequestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.security.web.util.RedirectUrlBuilder;

public class DefaultLoginUrlAuthenticationEntryPoint extends
		LoginUrlAuthenticationEntryPoint {

	static final String SAVED_REQUEST = "SPRING_SECURITY_SAVED_REQUEST";

	@Value("${ssoServerBase:}")
	private String ssoServerBase;

	public DefaultLoginUrlAuthenticationEntryPoint(String loginFormUrl) {
		super(loginFormUrl);
	}

	public void setSsoServerBase(String ssoServerBase) {
		this.ssoServerBase = ssoServerBase;
	}

	@Override
	protected String buildRedirectUrlToLoginPage(HttpServletRequest request,
			HttpServletResponse response, AuthenticationException authException) {
		return buildRedirectUrlToLoginPage(request);
	}

	public String buildRedirectUrlToLoginPage(HttpServletRequest request) {
		String targetUrl = null;
		String redirectUrl = null;
		SavedRequest savedRequest = (SavedRequest) request.getSession()
				.getAttribute(SAVED_REQUEST);
		request.getSession().removeAttribute(SAVED_REQUEST);
		if (savedRequest != null) {
			if (savedRequest instanceof DefaultSavedRequest) {
				DefaultSavedRequest dsr = (DefaultSavedRequest) savedRequest;
				String queryString = dsr.getQueryString();
				// remove jquery ajax parameter
				if (StringUtils.isNotBlank(queryString))
					queryString = queryString.replaceFirst("&?_=\\d{13}", "");
				if (StringUtils.isBlank(queryString)) {
					targetUrl = dsr.getRequestURL();
				} else {
					targetUrl = new StringBuilder(dsr.getRequestURL())
							.append("?").append(queryString).toString();
				}
			} else
				targetUrl = savedRequest.getRedirectUrl();
		} else {
			String queryString = request.getQueryString();
			if (StringUtils.isBlank(queryString)) {
				targetUrl = request.getRequestURL().toString();
			} else {
				targetUrl = new StringBuilder(request.getRequestURL())
						.append("?").append(queryString).toString();
			}
		}
		StringBuilder loginUrl = new StringBuilder();
		if (StringUtils.isBlank(ssoServerBase)) {
			String baseUrl = RequestUtils.getBaseUrl(request);
			targetUrl = RequestUtils.trimPathParameter(targetUrl);
			if (StringUtils.isNotBlank(targetUrl)
					&& targetUrl.startsWith(baseUrl)) {
				targetUrl = targetUrl.substring(baseUrl.length());
				if (targetUrl.equals("/"))
					targetUrl = "";
			}
			URL url = null;
			try {
				url = new URL(request.getRequestURL().toString());
			} catch (MalformedURLException e) {

			}
			RedirectUrlBuilder urlBuilder = new RedirectUrlBuilder();
			String scheme = request.getScheme();
			if (StringUtils.isNotBlank(request.getHeader("X-Real-Scheme")))
				scheme = request.getHeader("X-Real-Scheme");
			int serverPort = getPortResolver().getServerPort(request);
			urlBuilder.setScheme(scheme);
			if (url != null)
				urlBuilder.setServerName(url.getHost());
			urlBuilder.setPort(serverPort);
			urlBuilder.setContextPath(request.getContextPath());
			urlBuilder.setPathInfo(getLoginFormUrl());
			if (isForceHttps() && "http".equals(scheme)
					|| "https".equals(scheme)) {
				urlBuilder.setScheme("https");
				Integer httpsPort = getPortMapper().lookupHttpsPort(serverPort);
				if (httpsPort == null)
					httpsPort = 443;
				urlBuilder.setPort(httpsPort);
			}
			loginUrl = new StringBuilder(urlBuilder.getUrl());
		} else {
			loginUrl = new StringBuilder(ssoServerBase)
					.append(getLoginFormUrl());
		}
		try {
			if (StringUtils.isNotBlank(targetUrl))
				loginUrl.append('?')
						.append(DefaultUsernamePasswordAuthenticationFilter.TARGET_URL)
						.append('=')
						.append(URLEncoder.encode(targetUrl, "UTF-8"));
			redirectUrl = loginUrl.toString();
			if (isForceHttps() && redirectUrl.startsWith("http://")) {
				URL url = new URL(redirectUrl);
				RedirectUrlBuilder urlBuilder = new RedirectUrlBuilder();
				urlBuilder.setScheme("https");
				urlBuilder.setServerName(url.getHost());
				Integer httpsPort = getPortMapper().lookupHttpsPort(
						url.getPort());
				if (httpsPort == null)
					httpsPort = 443;
				urlBuilder.setPort(httpsPort);
				urlBuilder.setPathInfo(url.getPath());
				urlBuilder.setQuery(url.getQuery());
				redirectUrl = urlBuilder.getUrl();
			}
		} catch (Exception e) {
			redirectUrl = loginUrl.toString();
		}
		return redirectUrl;
	}
}
