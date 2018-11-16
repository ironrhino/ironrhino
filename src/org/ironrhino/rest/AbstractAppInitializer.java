package org.ironrhino.rest;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.ironrhino.core.servlet.DelegatingFilter;
import org.ironrhino.core.spring.NameGenerator;
import org.ironrhino.core.spring.servlet.InheritedDispatcherServlet;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

@SuppressWarnings("unchecked")
public abstract class AbstractAppInitializer<T extends ApiConfigBase> implements WebApplicationInitializer {

	private Class<T> apiConfigClass;

	public AbstractAppInitializer() {
		Type type = getClass().getGenericSuperclass();
		while (type != null) {
			if (type instanceof ParameterizedType) {
				apiConfigClass = (Class<T>) ((ParameterizedType) type).getActualTypeArguments()[0];
				break;
			} else if (type instanceof Class) {
				type = ((Class<?>) type).getGenericSuperclass();
			}
		}
	}

	@Override
	public final void onStartup(ServletContext servletContext) throws ServletException {
		String servletName = getServletName(apiConfigClass);
		ServletRegistration.Dynamic dynamic = servletContext.addServlet(servletName, InheritedDispatcherServlet.class);
		dynamic.setInitParameter(ContextLoader.CONTEXT_CLASS_PARAM,
				AnnotationConfigWebApplicationContext.class.getName());
		dynamic.setInitParameter(ContextLoader.CONFIG_LOCATION_PARAM, apiConfigClass.getName());
		dynamic.setAsyncSupported(true);
		dynamic.setMultipartConfig(createMultipartConfig());
		dynamic.setLoadOnStartup(1);
		try {
			// glassfish5 disallow add mapping after servlet context initialized
			T ac = apiConfigClass.getConstructor().newInstance();
			dynamic.addMapping(ac.getServletMapping());
		} catch (Exception e) {
			e.printStackTrace();
		}
		String filterName = NameGenerator.buildDefaultBeanName(RestFilter.class.getName());
		FilterRegistration filterRegistration = servletContext.getFilterRegistration(filterName);
		if (filterRegistration == null) {
			FilterRegistration.Dynamic dynamicFilter = servletContext.addFilter("restFilter", DelegatingFilter.class);
			dynamicFilter.setAsyncSupported(true);
			dynamicFilter.addMappingForServletNames(EnumSet.of(DispatcherType.REQUEST), true, servletName);
		} else {
			filterRegistration.addMappingForServletNames(EnumSet.of(DispatcherType.REQUEST), true, servletName);
		}
	}

	protected String getServletName(Class<?> apiConfigClass) {
		return apiConfigClass.getName();
	}

	protected MultipartConfigElement createMultipartConfig() {
		return new MultipartConfigElement(System.getProperty("java.io.tmpdir", "/tmp"), 4 * 1024 * 1024,
				5 * 1024 * 1024, 0);
	}
}