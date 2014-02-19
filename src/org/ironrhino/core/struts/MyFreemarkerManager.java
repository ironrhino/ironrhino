package org.ironrhino.core.struts;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts2.views.freemarker.FreemarkerManager;
import org.apache.struts2.views.freemarker.ScopesHashModel;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.AppInfo.Stage;
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
import freemarker.core.Environment;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.SimpleMapModel;
import freemarker.template.Configuration;
import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateModelException;

public class MyFreemarkerManager extends FreemarkerManager {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	@Override
	protected freemarker.template.Configuration createConfiguration(
			ServletContext servletContext) throws TemplateException {
		// Configuration configuration =
		// super.createConfiguration(servletContext);
		/** super.createConfiguration(servletContext) start **/
		if (AppInfo.getStage() == Stage.DEVELOPMENT)
			LocalizedTextUtil.setReloadBundles(true);
		MyConfiguration configuration = new MyConfiguration();
		configuration
				.setTemplateExceptionHandler(AppInfo.getStage() == Stage.PRODUCTION ? TemplateExceptionHandler.IGNORE_HANDLER
						: TemplateExceptionHandler.HTML_DEBUG_HANDLER);
		if (mruMaxStrongSize > 0) {
			configuration.setSetting(Configuration.CACHE_STORAGE_KEY, "strong:"
					+ mruMaxStrongSize);
		}
		if (templateUpdateDelay != null) {
			configuration.setSetting(Configuration.TEMPLATE_UPDATE_DELAY_KEY,
					templateUpdateDelay);
		}
		if (encoding != null) {
			configuration.setDefaultEncoding(encoding);
		}
		configuration.setWhitespaceStripping(true);
		/** super.createConfiguration(servletContext) end **/
		configuration
				.setOverridableTemplateProviders(WebApplicationContextUtils
						.getWebApplicationContext(servletContext)
						.getBeansOfType(OverridableTemplateProvider.class)
						.values());
		configuration.setFallbackTemplateProviders(WebApplicationContextUtils
				.getWebApplicationContext(servletContext)
				.getBeansOfType(FallbackTemplateProvider.class).values());
		TemplateProvider templateProvider = WebApplicationContextUtils
				.getWebApplicationContext(servletContext).getBean(
						"templateProvider", TemplateProvider.class);
		Map<String, Object> globalVariables = new HashMap<String, Object>(8);
		globalVariables.putAll(templateProvider.getAllSharedVariables());
		globalVariables.put("statics", BeansWrapper.getDefaultInstance()
				.getStaticModels());
		TemplateHashModelEx hash = new SimpleMapModel(globalVariables,
				BeansWrapper.getDefaultInstance());
		configuration.setAllSharedVariables(hash);
		configuration.setDateFormat("yyyy-MM-dd");
		configuration.setDateTimeFormat("yyyy-MM-dd HH:mm:ss");
		configuration.setNumberFormat("0.##");
		configuration.setURLEscapingCharset("UTF-8");
		if (AppInfo.getStage() == Stage.DEVELOPMENT)
			configuration.setSetting(Configuration.TEMPLATE_UPDATE_DELAY_KEY,
					"5");
		configuration.setCacheStorage(new StrongCacheStorage());
		configuration
				.setTemplateExceptionHandler(new TemplateExceptionHandler() {
					@Override
					public void handleTemplateException(TemplateException ex,
							Environment env, Writer writer)
							throws TemplateException {
						log.error(ex.getMessage());
					}
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
			searchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX
					+ ftlClasspath + "/meta/import/*.ftl";
			resources = resourcePatternResolver.getResources(searchPath);
			for (Resource r : resources) {
				location = r.getURL().toString();
				namespace = location.substring(location.lastIndexOf('/') + 1);
				namespace = namespace.substring(0, namespace.indexOf('.'));
				configuration.addAutoImport(namespace,
						location.substring(location.indexOf(ftlClasspath)));
			}
		} catch (IOException e) {
			log.debug(e.getMessage());
		}
		try {
			searchPath = ftlLocation + "/meta/import/*.ftl";
			resources = servletContextResourcePatternResolver
					.getResources(searchPath);
			for (Resource r : resources) {
				location = r.getURL().toString();
				namespace = location.substring(location.lastIndexOf('/') + 1);
				namespace = namespace.substring(0, namespace.indexOf('.'));
				configuration.addAutoImport(namespace,
						location.substring(location.indexOf(ftlLocation)));
			}
		} catch (IOException e) {
			log.debug(e.getMessage());
		}
		try {
			searchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX
					+ ftlClasspath + "/meta/include/*.ftl";
			resources = resourcePatternResolver.getResources(searchPath);
			for (Resource r : resources) {
				location = r.getURL().toString();
				configuration.addAutoInclude(location.substring(location
						.indexOf(ftlClasspath)));
			}
		} catch (IOException e) {
			log.debug(e.getMessage());
		}
		try {
			searchPath = ftlLocation + "/meta/include/*.ftl";
			resources = servletContextResourcePatternResolver
					.getResources(searchPath);
			for (Resource r : resources) {
				location = r.getURL().toString();
				configuration.addAutoInclude(location.substring(location
						.indexOf(ftlLocation)));
			}
		} catch (IOException e) {
			log.debug(e.getMessage());
		}
		return configuration;
	}

	@Override
	protected ScopesHashModel buildScopesHashModel(
			ServletContext servletContext, HttpServletRequest request,
			HttpServletResponse response, ObjectWrapper wrapper,
			ValueStack stack) {
		ScopesHashModel model = super.buildScopesHashModel(servletContext,
				request, response, wrapper, stack);
		try {
			model.put("Parameters", model.get(".freemarker.RequestParameters"));
		} catch (TemplateModelException e) {
			e.printStackTrace();
		}
		return model;
	}

}
