package org.ironrhino.core.struts;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.views.freemarker.FreemarkerManager;
import org.apache.struts2.views.freemarker.ScopesHashModel;
import org.ironrhino.core.freemarker.BeansTemplateHashModel;
import org.ironrhino.core.freemarker.FallbackTemplateProvider;
import org.ironrhino.core.freemarker.MyBeansWrapperBuilder;
import org.ironrhino.core.freemarker.MyConfiguration;
import org.ironrhino.core.freemarker.OverridableTemplateProvider;
import org.ironrhino.core.freemarker.PropertiesTemplateHashModel;
import org.ironrhino.core.freemarker.TemplateProvider;
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

import freemarker.cache.StrongCacheStorage;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.SimpleMapModel;
import freemarker.ext.servlet.HttpRequestHashModel;
import freemarker.ext.servlet.HttpRequestParametersHashModel;
import freemarker.ext.servlet.HttpSessionHashModel;
import freemarker.ext.servlet.ServletContextHashModel;
import freemarker.template.Configuration;
import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.Version;

public class MyFreemarkerManager extends FreemarkerManager {

	public static final Version DEFAULT_VERSION = Configuration.VERSION_2_3_25;

	public static final BeansWrapper DEFAULT_BEANS_WRAPPER = new MyBeansWrapperBuilder(DEFAULT_VERSION).build();

	private static final String ATTR_APPLICATION_MODEL = ".freemarker.Application";
	private static final String ATTR_SESSION_MODEL = ".freemarker.Session";
	private static final String ATTR_REQUEST_MODEL = ".freemarker.Request";
	private static final String ATTR_REQUEST_PARAMETERS_MODEL = ".freemarker.RequestParameters";

	public static final String KEY_BASE = "base";
	public static final String KEY_STATICS = "statics";
	public static final String KEY_BEANS = "beans";
	public static final String KEY_PROPERTIES = "properties";
	public static final String KEY_DEV_MODE = "devMode";
	public static final String KEY_FLUID_LAYOUT = "fluidLayout";

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	private String base;

	@Override
	protected freemarker.template.Configuration createConfiguration(ServletContext servletContext)
			throws TemplateException {
		// Configuration configuration =
		// super.createConfiguration(servletContext);
		/** super.createConfiguration(servletContext) start **/
		boolean devMode = AppInfo.getStage() == Stage.DEVELOPMENT;
		if (devMode)
			LocalizedTextUtil.setReloadBundles(true);
		MyConfiguration configuration = new MyConfiguration(DEFAULT_VERSION);
		configuration.setTemplateExceptionHandler(AppInfo.getStage() == Stage.PRODUCTION
				? TemplateExceptionHandler.IGNORE_HANDLER : TemplateExceptionHandler.HTML_DEBUG_HANDLER);
		if (mruMaxStrongSize > 0) {
			configuration.setSetting(Configuration.CACHE_STORAGE_KEY, "strong:" + mruMaxStrongSize);
		}
		if (templateUpdateDelay != null) {
			configuration.setSetting(Configuration.TEMPLATE_UPDATE_DELAY_KEY, templateUpdateDelay);
		}
		if (encoding != null) {
			configuration.setDefaultEncoding(encoding);
		}
		configuration.setWhitespaceStripping(true);
		/** super.createConfiguration(servletContext) end **/
		configuration.setOverridableTemplateProviders(WebApplicationContextUtils
				.getWebApplicationContext(servletContext).getBeansOfType(OverridableTemplateProvider.class).values());
		configuration.setFallbackTemplateProviders(WebApplicationContextUtils.getWebApplicationContext(servletContext)
				.getBeansOfType(FallbackTemplateProvider.class).values());
		TemplateProvider templateProvider = WebApplicationContextUtils.getWebApplicationContext(servletContext)
				.getBean("templateProvider", TemplateProvider.class);
		base = templateProvider.getAllSharedVariables().get(KEY_BASE);
		Map<String, Object> globalVariables = new HashMap<>(8);
		globalVariables.putAll(templateProvider.getAllSharedVariables());
		globalVariables.put(KEY_STATICS, DEFAULT_BEANS_WRAPPER.getStaticModels());
		globalVariables.put(KEY_BEANS, new BeansTemplateHashModel());
		globalVariables.put(KEY_PROPERTIES, new PropertiesTemplateHashModel());
		globalVariables.put(KEY_DEV_MODE, devMode);
		globalVariables.put(KEY_FLUID_LAYOUT,
				"true".equals(AppInfo.getApplicationContextProperties().get(KEY_FLUID_LAYOUT)));
		TemplateHashModelEx hash = new SimpleMapModel(globalVariables, DEFAULT_BEANS_WRAPPER);
		configuration.setAllSharedVariables(hash);
		configuration.setDateFormat("yyyy-MM-dd");
		configuration.setTimeFormat("HH:mm:ss");
		configuration.setDateTimeFormat("yyyy-MM-dd HH:mm:ss");
		configuration.setNumberFormat("0.##");
		configuration.setURLEscapingCharset("UTF-8");
		if (AppInfo.getStage() == Stage.DEVELOPMENT)
			configuration.setSetting(Configuration.TEMPLATE_UPDATE_DELAY_KEY, "5");
		configuration.setCacheStorage(new StrongCacheStorage());
		configuration.setTemplateExceptionHandler((ex, env, writer) -> {
			logger.error(ex.getMessage());
		});
		ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
		ServletContextResourcePatternResolver servletContextResourcePatternResolver = new ServletContextResourcePatternResolver(
				servletContext);
		Resource[] resources;
		String searchPath;
		String location;
		String namespace;
		String ftlClasspath = templateProvider.getFtlClasspath();
		String ftlLocation = templateProvider.getFtlLocation();
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
	public ScopesHashModel buildTemplateModel(ValueStack stack, Object action, ServletContext servletContext,
			HttpServletRequest request, HttpServletResponse response, ObjectWrapper wrapper) {
		ScopesHashModel model = super.buildTemplateModel(stack, action, servletContext, request, response, wrapper);
		if (StringUtils.isNotBlank(base))
			model.put(KEY_BASE, base);
		return model;
	}

	@SuppressWarnings("deprecation")
	@Override
	protected ScopesHashModel buildScopesHashModel(ServletContext servletContext, HttpServletRequest request,
			HttpServletResponse response, ObjectWrapper wrapper, ValueStack stack) {
		ScopesHashModel model = new ScopesHashModel(wrapper, servletContext, request, stack);
		String value = RequestUtils.getCookieValue(request, KEY_FLUID_LAYOUT);
		if ("true".equals(value)) {
			model.put(KEY_FLUID_LAYOUT, true);
		} else if ("false".equals(value)) {
			model.put(KEY_FLUID_LAYOUT, false);
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