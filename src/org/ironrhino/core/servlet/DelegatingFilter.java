package org.ironrhino.core.servlet;

import javax.servlet.Filter;
import javax.servlet.ServletException;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;

public class DelegatingFilter extends DelegatingFilterProxy {

	private static Filter dummy = new DummyFilter();

	protected Filter initDelegate(WebApplicationContext wac)
			throws ServletException {
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

}
