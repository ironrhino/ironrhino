package org.ironrhino.rest;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.servlet.DelegatingFilter;
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
	public void onStartup(ServletContext servletContext) throws ServletException {
		String version = "";
		try {
			version = apiConfigClass.getConstructor().newInstance().getVersion();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
		}
		String servletName = getServletName(version);
		String servletMapping = getServletMapping(version);
		ServletRegistration.Dynamic dynamic = servletContext.addServlet(servletName, InheritedDispatcherServlet.class);
		dynamic.setInitParameter(ContextLoader.CONTEXT_CLASS_PARAM,
				AnnotationConfigWebApplicationContext.class.getName());
		dynamic.setInitParameter(ContextLoader.CONFIG_LOCATION_PARAM, apiConfigClass.getName());
		dynamic.addMapping(servletMapping);
		dynamic.setAsyncSupported(true);
		dynamic.setMultipartConfig(createMultipartConfig());
		dynamic.setLoadOnStartup(1);
		String filterName = "restFilter";
		FilterRegistration filterRegistration = servletContext.getFilterRegistration(filterName);
		if (filterRegistration == null) {
			FilterRegistration.Dynamic dynamicFilter = servletContext.addFilter("restFilter", DelegatingFilter.class);
			dynamicFilter.setAsyncSupported(true);
			dynamicFilter.addMappingForServletNames(EnumSet.of(DispatcherType.REQUEST), true, servletName);
		} else {
			filterRegistration.addMappingForServletNames(EnumSet.of(DispatcherType.REQUEST), true, servletName);
		}
	}

	protected String getServletName(String version) {
		return "api" + (StringUtils.isNotBlank(version) ? "-" + version : "");
	}

	protected String getServletMapping(String version) {
		return "/api" + (StringUtils.isNotBlank(version) ? "/" + version : "") + "/*";
	}

	protected MultipartConfigElement createMultipartConfig() {
		return new MultipartConfigElement(System.getProperty("java.io.tmpdir", "/tmp"), 4 * 1024 * 1024,
				16 * 1024 * 1024, 0);
	}
}