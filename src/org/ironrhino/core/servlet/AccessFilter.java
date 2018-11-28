package org.ironrhino.core.servlet;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.metrics.Metrics;
import org.ironrhino.core.session.HttpSessionManager;
import org.ironrhino.core.spring.security.DefaultAuthenticationSuccessHandler;
import org.ironrhino.core.tracing.HttpServletRequestTextMap;
import org.ironrhino.core.tracing.Tracing;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.CodecUtils;
import org.ironrhino.core.util.RequestUtils;
import org.ironrhino.core.util.UserAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.Tracer.SpanBuilder;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class AccessFilter implements Filter {

	public static final String HTTP_HEADER_INSTANCE_ID = "X-Instance-Id";
	public static final String HTTP_HEADER_REQUEST_ID = "X-Request-Id";
	public static final String MDC_KEY_REQUEST_ID = "requestId";
	public static final String HTTP_HEADER_REQUEST_CHAIN = "X-Request-Chain";
	public static final String MDC_KEY_REQUEST_CHAIN = "requestChain";
	public static final String HTTP_HEADER_REQUEST_FROM = "X-Request-From";
	public static final String MDC_KEY_REQUEST_FROM = "requestFrom";

	private Logger accessLog = LoggerFactory.getLogger("access");

	private Logger accesWarnLog = LoggerFactory.getLogger("access-warn");

	public static final long DEFAULT_RESPONSETIMETHRESHOLD = 5000;

	public static final boolean DEFAULT_PRINT = true;

	@Getter
	@Setter
	@Value("${accessFilter.responseTimeThreshold:" + DEFAULT_RESPONSETIMETHRESHOLD + "}")
	public long responseTimeThreshold = DEFAULT_RESPONSETIMETHRESHOLD;

	@Setter
	@Value("${accessFilter.print:" + DEFAULT_PRINT + "}")
	private boolean print = DEFAULT_PRINT;

	@Setter
	@Value("${accessFilter.excludePatterns:}")
	private String excludePatterns;

	private List<String> excludePatternsList = Collections.emptyList();

	@Autowired(required = false)
	private List<AccessHandler> handlers;

	@Autowired
	private HttpSessionManager httpSessionManager;

	@PostConstruct
	public void _init() {
		if (StringUtils.isNotBlank(excludePatterns))
			excludePatternsList = Arrays.asList(excludePatterns.split("\\s*,\\s*"));
	}

	@Override
	public void init(FilterConfig filterConfig) {
		_init();
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
			throws IOException, ServletException {
		boolean isRequestDispatcher = req.getDispatcherType() == DispatcherType.REQUEST;
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) resp;
		if (ProxySupportHttpServletRequest.isProxyable(request)) {
			request = new ProxySupportHttpServletRequest(request);
			response = new ProxySupportHttpServletResponse(request, response);
		}
		LocaleContextHolder.setLocale(request.getLocale(), true);
		String uri = request.getRequestURI();
		uri = uri.substring(request.getContextPath().length());

		Span span = null;
		Scope scope = null;
		if (!uri.startsWith("/assets/") && Tracing.isEnabled()) {
			Tracer tracer = GlobalTracer.get();
			SpanBuilder spanBuilder = tracer.buildSpan("http:" + request.getMethod().toLowerCase() + ":" + uri)
					.asChildOf(tracer.extract(Format.Builtin.HTTP_HEADERS, new HttpServletRequestTextMap(request)));
			span = spanBuilder.start();
			Tags.HTTP_URL.set(span, request.getRequestURL().toString());
			Tags.HTTP_METHOD.set(span, request.getMethod());
			if (request.getDispatcherType() == DispatcherType.ASYNC)
				span.setTag("async", true);
			scope = tracer.activateSpan(span);
		}

		RequestContext.set(request, response);
		try {
			if (isRequestDispatcher && RequestUtils.isInternalTesting(request))
				response.addHeader(HTTP_HEADER_INSTANCE_ID, AppInfo.getInstanceId());

			for (String pattern : excludePatternsList) {
				if (org.ironrhino.core.util.StringUtils.matchesWildcard(uri, pattern)) {
					chain.doFilter(request, response);
					return;
				}
			}

			if (isRequestDispatcher && handlers != null)
				loop: for (AccessHandler handler : handlers) {
					String excludePattern = handler.getExcludePattern();
					if (StringUtils.isNotBlank(excludePattern)) {
						String[] arr = excludePattern.split("\\s*,\\s*");
						for (String pa : arr)
							if (org.ironrhino.core.util.StringUtils.matchesWildcard(uri, pa)) {
								continue loop;
							}
					}
					String pattern = handler.getPattern();
					boolean matched = StringUtils.isBlank(pattern);
					if (!matched) {
						String[] arr = pattern.split("\\s*,\\s*");
						for (String pa : arr)
							if (org.ironrhino.core.util.StringUtils.matchesWildcard(uri, pa)) {
								matched = true;
								break;
							}
					}
					if (matched) {
						if (handler.handle(request, response)) {
							return;
						}
					}
				}

			if (request.getAttribute("userAgent") == null)
				request.setAttribute("userAgent", new UserAgent(request.getHeader("User-Agent")));
			String remoteAddr = request.getRemoteAddr();
			String proxyAddr = (String) request
					.getAttribute(ProxySupportHttpServletRequest.REQUEST_ATTRIBUTE_PROXY_ADDR);
			if (proxyAddr != null)
				remoteAddr += " via " + proxyAddr;
			MDC.put("remoteAddr", remoteAddr);
			MDC.put("method", request.getMethod());
			StringBuffer url = request.getRequestURL();
			if (StringUtils.isNotBlank(request.getQueryString()))
				url.append('?').append(request.getQueryString());
			MDC.put("url", " " + url.toString());
			String s = request.getHeader("User-Agent");
			if (s == null)
				s = "";
			MDC.put("userAgent", " UserAgent:" + s);
			s = request.getHeader("Referer");
			if (s == null || !s.startsWith("http"))
				s = "";
			MDC.put("referer", " Referer:" + s);
			s = RequestUtils.getCookieValue(request, DefaultAuthenticationSuccessHandler.COOKIE_NAME_LOGIN_USER);
			if (s != null)
				MDC.put("username", s);
			String sessionId = null;
			if (httpSessionManager != null) {
				sessionId = httpSessionManager.getSessionId(request);
			}
			String requestId = (String) request.getAttribute(HTTP_HEADER_REQUEST_ID);
			String requestChain = null;
			if (requestId == null) {
				requestId = request.getHeader(HTTP_HEADER_REQUEST_ID);
				if (StringUtils.isBlank(requestId)) {
					requestId = CodecUtils.generateRequestId();
					response.setHeader(HTTP_HEADER_REQUEST_ID, requestId);
					requestChain = requestId.substring(14);
				}
				if (sessionId != null && !requestId.startsWith(sessionId + '.'))
					requestId = new StringBuilder(sessionId).append('.').append(requestId).toString();
				request.setAttribute(HTTP_HEADER_REQUEST_ID, requestId);
			}
			MDC.put(MDC_KEY_REQUEST_ID, requestId);
			StringBuilder sb = new StringBuilder();
			sb.append(" request:");
			sb.append(requestId);
			String originalChain = request.getHeader(HTTP_HEADER_REQUEST_CHAIN);
			if (originalChain != null) {
				requestChain = new StringBuilder(originalChain).append('.')
						.append(requestChain != null ? requestChain : CodecUtils.nextId(14)).toString();
				sb.append(" chain:");
				sb.append(requestChain);
			}
			MDC.put(MDC_KEY_REQUEST_CHAIN, requestChain);
			String requestFrom = request.getHeader(HTTP_HEADER_REQUEST_FROM);
			if (requestFrom != null) {
				// sb.append(" from:");
				// sb.append(requestFrom);
				MDC.put(MDC_KEY_REQUEST_FROM, requestFrom);
			}
			MDC.put("request", sb.toString());

			MDC.put("server", " server:" + AppInfo.getInstanceId(true));
			long start = System.currentTimeMillis();
			try {
				chain.doFilter(request, response);
				long responseTime = System.currentTimeMillis() - start;
				if (isRequestDispatcher && responseTime > responseTimeThreshold) {
					StringBuilder msg = new StringBuilder();
					msg.append(RequestUtils.serializeData(request)).append(" response time:").append(responseTime)
							.append("ms");
					accesWarnLog.warn(msg.toString());
					Metrics.recordTimer("http.access.slow", responseTime, TimeUnit.MILLISECONDS, "uri", uri);
				}
			} catch (ServletException e) {
				log.error(e.getMessage(), e);
				throw e;
			} finally {
				if (isRequestDispatcher && print && !uri.startsWith("/assets/") && !uri.startsWith("/remoting/")
						&& request.getHeader("Last-Event-ID") == null) {
					long responseTime = System.currentTimeMillis() - start;
					MDC.put("responseTime", " responseTime:" + responseTime);
					accessLog.info("");
					Metrics.recordTimer("http.access", responseTime, TimeUnit.MILLISECONDS);
				}
				MDC.clear();
			}
		} catch (Exception e) {
			Tracing.logError(e);
			throw e;
		} finally {
			if (span != null) {
				Tags.HTTP_STATUS.set(span, response.getStatus());
				scope.close();
				span.finish();
			}
			RequestContext.reset();
			LocaleContextHolder.resetLocaleContext();
		}
	}

	@Override
	public void destroy() {

	}

}
