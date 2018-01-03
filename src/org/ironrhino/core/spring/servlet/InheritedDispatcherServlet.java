package org.ironrhino.core.spring.servlet;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

public class InheritedDispatcherServlet extends DispatcherServlet {

	private static final long serialVersionUID = 0L;

	public InheritedDispatcherServlet() {
		super();
	}

	public InheritedDispatcherServlet(WebApplicationContext webApplicationContext) {
		super(webApplicationContext);
	}

	@Override
	protected void configureAndRefreshWebApplicationContext(ConfigurableWebApplicationContext wac) {
		ApplicationContext parent = wac.getParent();
		if (parent != null) {
			try {
				PropertySourcesPlaceholderConfigurer pspc = parent.getBean(PropertySourcesPlaceholderConfigurer.class);
				wac.addBeanFactoryPostProcessor(pspc);
			} catch (NoSuchBeanDefinitionException e) {
			}
		}
		super.configureAndRefreshWebApplicationContext(wac);
	}

}
