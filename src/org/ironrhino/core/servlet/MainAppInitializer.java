package org.ironrhino.core.servlet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.TimeZone;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.ironrhino.core.util.AppInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.util.IntrospectorCleanupListener;

@Order(Ordered.HIGHEST_PRECEDENCE)
public class MainAppInitializer implements WebApplicationInitializer {

	public static ServletContext SERVLET_CONTEXT;

	private Logger logger;

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		AppInfo.initialize();
		SERVLET_CONTEXT = servletContext;
		String context = SERVLET_CONTEXT.getRealPath("/");
		if (context == null)
			context = "";
		System.setProperty("app.context", context);
		String defaultProfiles = System.getProperty(AbstractEnvironment.DEFAULT_PROFILES_PROPERTY_NAME);
		logger = LoggerFactory.getLogger(getClass());
		printVersion(servletContext);
		if (AppInfo.getHttpPort() == 0) {
			int port = ContainerDetector.detectHttpPort(servletContext, false);
			if (port > 0) {
				AppInfo.setHttpPort(port);
				logger.info("Server http port auto detected: {}", port);
			}
		}
		if (AppInfo.getHttpsPort() == 0) {
			int port = ContainerDetector.detectHttpPort(servletContext, true);
			if (port > 0) {
				AppInfo.setHttpsPort(port);
				logger.info("Server https port auto detected: {}", port);
			}
		}
		System.setProperty(AppInfo.KEY_APP_INSTANCEID, AppInfo.getInstanceId(true));
		logger.info("Default timezone {}", TimeZone.getDefault().getID());
		logger.info(
				"app.name={},app.version={},app.instanceid={},app.stage={},app.runlevel={},app.home={},hostname={},hostaddress={},profiles={}",
				AppInfo.getAppName(), AppInfo.getAppVersion(), AppInfo.getInstanceId(), AppInfo.getStage().toString(),
				AppInfo.getRunLevel().toString(), AppInfo.getAppHome(), AppInfo.getHostName(), AppInfo.getHostAddress(),
				defaultProfiles != null ? defaultProfiles : "default");
		configure(servletContext);
	}

	private void configure(ServletContext servletContext) {

		FilterRegistration.Dynamic filterDynamic;

		filterDynamic = servletContext.addFilter("characterEncodingFilter", CharacterEncodingFilter.class);
		filterDynamic.setAsyncSupported(true);
		filterDynamic.setInitParameter("encoding", StandardCharsets.UTF_8.name());
		filterDynamic.setInitParameter("forceEncoding", Boolean.toString(true));
		filterDynamic.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, "/*");

		filterDynamic = servletContext.addFilter("captchaFilter", DelegatingFilter.class);
		filterDynamic.setAsyncSupported(true);
		filterDynamic.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");

		filterDynamic = servletContext.addFilter("accessFilter", DelegatingFilter.class);
		filterDynamic.setAsyncSupported(true);
		filterDynamic.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC), true, "/*");

		filterDynamic = servletContext.addFilter("httpSessionFilter", DelegatingFilter.class);
		filterDynamic.setAsyncSupported(true);
		filterDynamic.setInitParameter("targetFilterLifecycle", Boolean.toString(true));
		filterDynamic.setInitParameter("excludePatterns", "/assets/*,/remoting/*");
		filterDynamic.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");

		filterDynamic = servletContext.addFilter("springSecurityFilterChain", DelegatingFilter.class);
		filterDynamic.setAsyncSupported(true);
		filterDynamic.setInitParameter("excludePatterns", "/assets/*,/remoting/*");
		filterDynamic.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC), true, "/*");

		filterDynamic = servletContext.addFilter("openSessionInViewFilter", DelegatingFilter.class);
		filterDynamic.setAsyncSupported(true);
		filterDynamic.setInitParameter("targetFilterLifecycle", Boolean.toString(true));
		filterDynamic.setInitParameter("excludePatterns", "/assets/*,/remoting/*");
		filterDynamic.setInitParameter("singleSession", Boolean.toString(true));
		filterDynamic.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC), true, "/*");

		filterDynamic = servletContext.addFilter("strutsPrepareFilter", DelegatingFilter.class);
		filterDynamic.setAsyncSupported(true);
		filterDynamic.setInitParameter("targetFilterLifecycle", Boolean.toString(true));
		filterDynamic.setInitParameter("excludePatterns", "/assets/*,/remoting/*,/api/*");
		filterDynamic.addMappingForUrlPatterns(
				EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.ERROR), true, "/*");

		filterDynamic = servletContext.addFilter("sitemeshFilter", DelegatingFilter.class);
		filterDynamic.setAsyncSupported(true);
		filterDynamic.setInitParameter("targetFilterLifecycle", Boolean.toString(true));
		filterDynamic.setInitParameter("excludePatterns", "/assets/*,/remoting/*,/api/*");
		filterDynamic.setInitParameter("configFile", "resources/sitemesh/sitemesh.xml");
		filterDynamic.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST, DispatcherType.ERROR), true, "/*");

		filterDynamic = servletContext.addFilter("strutsExecuteFilter", DelegatingFilter.class);
		filterDynamic.setAsyncSupported(true);
		filterDynamic.setInitParameter("targetFilterLifecycle", Boolean.toString(true));
		filterDynamic.setInitParameter("excludePatterns", "/assets/*,/remoting/*,/api/*");
		filterDynamic.addMappingForUrlPatterns(
				EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.ERROR), true, "/*");

		ServletRegistration.Dynamic servletDynamic = servletContext.addServlet("test", TestServlet.class);
		servletDynamic.setLoadOnStartup(100);

		servletContext.addListener(IntrospectorCleanupListener.class);
	}

	private void printVersion(ServletContext servletContext) {
		for (String path : servletContext.getResourcePaths("/WEB-INF/lib")) {
			String filename = path.substring(path.lastIndexOf('/') + 1);
			if (filename.startsWith("ironrhino-core-") && filename.endsWith(".jar")) {
				try (JarInputStream jis = new JarInputStream(servletContext.getResourceAsStream(path))) {
					Manifest mf = jis.getManifest();
					if (mf != null) {
						Attributes attr = mf.getMainAttributes();
						String version = attr.getValue("Implementation-Version");
						String revision = attr.getValue("Build-Revision");
						logger.info("You are running with Ironrhino Core: version={}, revision={}", version, revision);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				break;
			}
		}
	}
}