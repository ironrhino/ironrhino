package org.ironrhino.core.cache;

import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import org.ironrhino.core.servlet.RequestContext;
import org.ironrhino.core.util.ApplicationContextUtils;
import org.ironrhino.core.util.ExpressionUtils;
import org.ironrhino.core.util.HtmlUtils;
import org.ironrhino.core.util.HtmlUtils.Replacer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opensymphony.xwork2.ActionContext;

import ognl.OgnlContext;

public class CacheContext {

	private static Logger logger = LoggerFactory.getLogger(CacheContext.class);

	public static final String FORCE_FLUSH_PARAM_NAME = "_ff_";

	public static final String DEFAULT_SCOPE = "application";

	public static final String NAMESPACE_PAGE_FRAGMENT = "pageFragment";

	private static CacheManager cacheManager;

	private static CacheManager getCacheManager() {
		if (cacheManager == null)
			cacheManager = (CacheManager) ApplicationContextUtils.getBean("cacheManager");
		return cacheManager;
	}

	public static boolean isForceFlush() {
		try {
			return RequestContext.getRequest() != null
					&& RequestContext.getRequest().getParameter(FORCE_FLUSH_PARAM_NAME) != null;
		} catch (Exception e) {
			return false;
		}
	}

	public static String getPageFragment(String key, String scope) {
		try {
			Object actualKey = eval(key);
			if (actualKey == null || CacheContext.isForceFlush())
				return null;
			key = actualKey.toString();
			scope = eval(scope).toString();
			String content = (String) getCacheManager().get(completeKey(key, scope), NAMESPACE_PAGE_FRAGMENT);
			if (content != null) {
				if (RequestContext.getRequest().isRequestedSessionIdFromCookie())
					return content;
				else
					return HtmlUtils.process(content, replacer);
			}
			return null;
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
			return null;
		}
	}

	static Replacer replacer = (st, attr) -> {
		if (!st.getName().equals("a") || !attr.getKey().equals("href"))
			return null;
		String href = attr.getValue();
		StringBuilder sb = new StringBuilder().append(attr.getName()).append("=\"");
		sb.append(RequestContext.getResponse().encodeURL(href));
		sb.append("\"");
		return sb.toString();
	};

	public static void putPageFragment(String key, String content, String scope, String timeToIdle, String timeToLive) {
		Object actualKey = eval(key);
		if (actualKey == null)
			return;
		try {
			key = actualKey.toString();
			scope = eval(scope).toString();
			int _timeToIdle = Integer.valueOf(eval(timeToIdle).toString());
			int _timeToLive = Integer.valueOf(eval(timeToLive).toString());
			getCacheManager().put(completeKey(key, scope), content, _timeToIdle, _timeToLive, TimeUnit.SECONDS,
					NAMESPACE_PAGE_FRAGMENT);
		} catch (Throwable e) {
		}
	}

	private static String completeKey(String key, String scope) {
		HttpServletRequest request = RequestContext.getRequest();
		StringBuilder sb = new StringBuilder();
		sb.append(key);
		if (scope.equalsIgnoreCase("session"))
			sb.append("," + request.getSession(true).getId());
		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	public static Object eval(String template) {
		if (template == null)
			return null;
		template = template.trim();
		OgnlContext ognl = (OgnlContext) ActionContext.getContext().getContextMap();
		Object value = ExpressionUtils.eval(template, ognl.getValues());
		return value;
	}

}
