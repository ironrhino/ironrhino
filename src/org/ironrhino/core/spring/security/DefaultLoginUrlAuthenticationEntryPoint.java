package org.ironrhino.core.spring.security;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.security.SecurityConfig;
import org.ironrhino.core.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.RedirectUrlBuilder;

public class DefaultLoginUrlAuthenticationEntryPoint extends LoginUrlAuthenticationEntryPoint {

	@Value("${login.ignoreSavedRequest:false}")
	private boolean ignoreSavedRequest;

	private String ssoServerBase = "";

	@Autowired(required = false)
	private SecurityConfig securityConfig;

	@Autowired(required = false)
	private List<LoginEntryPointHandler> loginEntryPointHandlers;

	public DefaultLoginUrlAuthenticationEntryPoint(String loginFormUrl) {
		super(loginFormUrl);
	}

	@PostConstruct
	private void init() {
		if (securityConfig != null)
			ssoServerBase = org.ironrhino.core.util.StringUtils.trimTailSlash(securityConfig.getSsoServerBase());
	}

	@Override
	protected String buildRedirectUrlToLoginPage(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) {
		return buildRedirectUrlToLoginPage(request, response);
	}

	public String buildRedirectUrlToLoginPage(HttpServletRequest request, HttpServletResponse response) {
		String targetUrl = null;
		String redirectUrl = null;
		if (!ignoreSavedRequest) {
			String queryString = request.getQueryString();
			if (StringUtils.isBlank(queryString)) {
				targetUrl = request.getRequestURL().toString();
			} else {
				targetUrl = new StringBuilder(request.getRequestURL()).append("?").append(queryString).toString();
			}
			if (StringUtils.isBlank(ssoServerBase)) {
				String baseUrl = RequestUtils.getBaseUrl(request);
				targetUrl = RequestUtils.trimPathParameter(targetUrl);
				if (StringUtils.isNotBlank(targetUrl) && targetUrl.startsWith(baseUrl)) {
					targetUrl = targetUrl.substring(baseUrl.length());
					if (targetUrl.equals("/"))
						targetUrl = "";
				}
			}
		}

		if (loginEntryPointHandlers != null)
			for (LoginEntryPointHandler handler : loginEntryPointHandlers) {
				redirectUrl = handler.handle(request, targetUrl);
				if (StringUtils.isNotBlank(redirectUrl))
					return redirectUrl;
			}

		StringBuilder loginUrl = new StringBuilder();
		if (StringUtils.isBlank(ssoServerBase)) {
			RedirectUrlBuilder urlBuilder = new RedirectUrlBuilder();
			String scheme = request.getScheme();
			urlBuilder.setScheme(scheme);
			urlBuilder.setServerName(request.getServerName());
			int serverPort = getPortResolver().getServerPort(request);
			urlBuilder.setPort(serverPort);
			urlBuilder.setContextPath(request.getContextPath());
			urlBuilder.setPathInfo(getLoginFormUrl());
			if (isForceHttps() && !request.isSecure()) {
				urlBuilder.setScheme("https");
				Integer httpsPort = getPortMapper().lookupHttpsPort(serverPort);
				if (httpsPort == null)
					httpsPort = 443;
				urlBuilder.setPort(httpsPort);
			}
			loginUrl = new StringBuilder(urlBuilder.getUrl());
		} else {
			loginUrl = new StringBuilder(ssoServerBase).append(getLoginFormUrl());
		}
		try {
			if (StringUtils.isNotBlank(targetUrl))
				loginUrl.append('?').append(DefaultUsernamePasswordAuthenticationFilter.TARGET_URL).append('=')
						.append(URLEncoder.encode(targetUrl, "UTF-8"));
			redirectUrl = loginUrl.toString();
			if (isForceHttps() && redirectUrl.startsWith("http://")) {
				URI url = URI.create(redirectUrl);
				RedirectUrlBuilder urlBuilder = new RedirectUrlBuilder();
				urlBuilder.setScheme("https");
				urlBuilder.setServerName(url.getHost());
				Integer httpsPort = getPortMapper().lookupHttpsPort(url.getPort());
				if (httpsPort == null)
					httpsPort = 443;
				urlBuilder.setPort(httpsPort);
				urlBuilder.setPathInfo(url.getPath());
				urlBuilder.setQuery(url.getQuery());
				redirectUrl = urlBuilder.getUrl();
			}
		} catch (UnsupportedEncodingException e) {
			redirectUrl = loginUrl.toString();
		}
		return redirectUrl;
	}

}
