package org.ironrhino.core.struts;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.views.freemarker.FreemarkerManager;
import org.apache.struts2.views.freemarker.ScopesHashModel;
import org.ironrhino.core.freemarker.FreemarkerConfigurer;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.AppInfo.Stage;
import org.ironrhino.core.util.RequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.web.context.support.ServletContextResourcePatternResolver;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.opensymphony.xwork2.util.LocalizedTextUtil;
import com.opensymphony.xwork2.util.ValueStack;

import freemarker.ext.servlet.HttpRequestHashModel;
import freemarker.ext.servlet.HttpRequestParametersHashModel;
import freemarker.ext.servlet.HttpSessionHashModel;
import freemarker.ext.servlet.ServletContextHashModel;
import freemarker.template.Configuration;
import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateException;

public class MyFreemarkerManager extends FreemarkerManager {

	private static final String ATTR_APPLICATION_MODEL = ".freemarker.Application";
	private static final String ATTR_SESSION_MODEL = ".freemarker.Session";
	private static final String ATTR_REQUEST_MODEL = ".freemarker.Request";
	private static final String ATTR_REQUEST_PARAMETERS_MODEL = ".freemarker.RequestParameters";

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	private FreemarkerConfigurer freemarkerConfigurer;

	@Override
	protected freemarker.template.Configuration createConfiguration(ServletContext servletContext)
			throws TemplateException {
		boolean devMode = AppInfo.getStage() == Stage.DEVELOPMENT;
		if (devMode)
			LocalizedTextUtil.setReloadBundles(true);
		freemarkerConfigurer = WebApplicationContextUtils.getWebApplicationContext(servletContext)
				.getBean(FreemarkerConfigurer.class);
		Configuration configuration = freemarkerConfigurer.createConfiguration();
		ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
		ServletContextResourcePatternResolver servletContextResourcePatternResolver = new ServletContextResourcePatternResolver(
				servletContext);
		Resource[] resources;
		String searchPath;
		String location;
		String namespace;
		String ftlClasspath = freemarkerConfigurer.getFtlClasspath();
		String ftlLocation = freemarkerConfigurer.getFtlLocation();
		try {
			searchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + ftlClasspath + "/meta/import/*.ftl";
			resources = resourcePatternResolver.getResources(searchPath);
			for (Resource r : resources) {
				location = r.getURL().toString();
				namespace = location.substring(location.lastIndexOf('/') + 1);
				namespace = namespace.substring(0, namespace.indexOf('.'));
				configuration.addAutoImport(namespace, location.substring(location.indexOf(ftlClasspath)));
			}
		} catch (IOException e) {
			logger.debug(e.getMessage());
		}
		try {
			searchPath = ftlLocation + "/meta/import/*.ftl";
			resources = servletContextResourcePatternResolver.getResources(searchPath);
			for (Resource r : resources) {
				location = r.getURL().toString();
				namespace = location.substring(location.lastIndexOf('/') + 1);
				namespace = namespace.substring(0, namespace.indexOf('.'));
				configuration.addAutoImport(namespace, location.substring(location.indexOf(ftlLocation)));
			}
		} catch (IOException e) {
			logger.debug(e.getMessage());
		}
		try {
			searchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + ftlClasspath + "/meta/include/*.ftl";
			resources = resourcePatternResolver.getResources(searchPath);
			for (Resource r : resources) {
				location = r.getURL().toString();
				configuration.addAutoInclude(location.substring(location.indexOf(ftlClasspath)));
			}
		} catch (IOException e) {
			logger.debug(e.getMessage());
		}
		try {
			searchPath = ftlLocation + "/meta/include/*.ftl";
			resources = servletContextResourcePatternResolver.getResources(searchPath);
			for (Resource r : resources) {
				location = r.getURL().toString();
				configuration.addAutoInclude(location.substring(location.indexOf(ftlLocation)));
			}
		} catch (IOException e) {
			logger.debug(e.getMessage());
		}
		return configuration;
	}

	@Override
	protected ObjectWrapper createObjectWrapper(ServletContext servletContext) {
		return FreemarkerConfigurer.DEFAULT_BEANS_WRAPPER;
	}

	@Override
	public ScopesHashModel buildTemplateModel(ValueStack stack, Object action, ServletContext servletContext,
			HttpServletRequest request, HttpServletResponse response, ObjectWrapper wrapper) {
		ScopesHashModel model = super.buildTemplateModel(stack, action, servletContext, request, response, wrapper);
		if (StringUtils.isNotBlank(freemarkerConfigurer.getBase()))
			model.put(FreemarkerConfigurer.KEY_BASE, freemarkerConfigurer.getBase());
		return model;
	}

	@SuppressWarnings("deprecation")
	@Override
	protected ScopesHashModel buildScopesHashModel(ServletContext servletContext, HttpServletRequest request,
			HttpServletResponse response, ObjectWrapper wrapper, ValueStack stack) {
		ScopesHashModel model = new ScopesHashModel(wrapper, servletContext, request, stack);
		String value = RequestUtils.getCookieValue(request, FreemarkerConfigurer.KEY_FLUID_LAYOUT);
		if ("true".equals(value)) {
			model.put(FreemarkerConfigurer.KEY_FLUID_LAYOUT, true);
		} else if ("false".equals(value)) {
			model.put(FreemarkerConfigurer.KEY_FLUID_LAYOUT, false);
		}
		ServletContextHashModel servletContextModel = (ServletContextHashModel) servletContext
				.getAttribute(ATTR_APPLICATION_MODEL);
		if (servletContextModel == null) {
			servletContextModel = new ServletContextHashModel(servletContext, wrapper);
			servletContext.setAttribute(ATTR_APPLICATION_MODEL, servletContextModel);
		}
		model.put(KEY_APPLICATION, servletContextModel);
		// Create hash model wrapper for session
		HttpSession session = request.getSession(false);
		if (session != null) {
			HttpSessionHashModel sessionModel = (HttpSessionHashModel) request.getAttribute(ATTR_SESSION_MODEL);
			if (sessionModel == null) {
				sessionModel = new HttpSessionHashModel(session, wrapper);
				request.setAttribute(ATTR_SESSION_MODEL, sessionModel);
			}
			model.put(KEY_SESSION, sessionModel);
		}
		// Create hash model wrapper for the request attributes
		HttpRequestHashModel requestModel = (HttpRequestHashModel) request.getAttribute(ATTR_REQUEST_MODEL);
		if (requestModel == null) {
			requestModel = new HttpRequestHashModel(request, response, wrapper);
			request.setAttribute(ATTR_REQUEST_MODEL, requestModel);
		}
		model.put(KEY_REQUEST, requestModel);
		// Create hash model wrapper for request parameters
		HttpRequestParametersHashModel reqParametersModel = (HttpRequestParametersHashModel) request
				.getAttribute(ATTR_REQUEST_PARAMETERS_MODEL);
		if (reqParametersModel == null) {
			reqParametersModel = new HttpRequestParametersHashModel(request);
			request.setAttribute(ATTR_REQUEST_PARAMETERS_MODEL, reqParametersModel);
		}
		model.put(KEY_REQUEST_PARAMETERS_STRUTS, reqParametersModel);
		return model;
	}

}