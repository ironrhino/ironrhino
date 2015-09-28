package org.ironrhino.core.servlet;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.RequestUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;

public class DelegatingFilter extends DelegatingFilterProxy {

	private static Filter dummy = new DummyFilter();

	private List<String> excludePatternsList;

	@Override
	protected Filter initDelegate(WebApplicationContext wac) throws ServletException {
		String excludePatterns = getFilterConfig().getInitParameter("excludePatterns");
		String str = wac.getEnvironment().getProperty(getTargetBeanName() + ".excludePatterns");
		if (str != null)
			excludePatterns = str;
		if (StringUtils.isNotBlank(excludePatterns))
			excludePatternsList = Arrays.asList(excludePatterns.split("\\s*,\\s*"));
		try {
			Filter delegate = wac.getBean(getTargetBeanName(), Filter.class);
			if (isTargetFilterLifecycle()) {
				delegate.init(getFilterConfig());
			}
			return delegate;
		} catch (NoSuchBeanDefinitionException e) {
			logger.warn("Use a dummy filter instead: " + e.getMessage());
			return dummy;
		}

	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
			throws ServletException, IOException {
		String uri = RequestUtils.getRequestUri((HttpServletRequest) req);
		if (excludePatternsList != null)
			for (String pattern : excludePatternsList)
				if (org.ironrhino.core.util.StringUtils.matchesWildcard(uri, pattern)) {
					chain.doFilter(req, res);
					return;
				}
		super.doFilter(req, res, chain);
	}

}
