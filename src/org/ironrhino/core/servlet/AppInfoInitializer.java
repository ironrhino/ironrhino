package org.ironrhino.core.servlet;

import java.util.TimeZone;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.ironrhino.core.util.AppInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.web.WebApplicationInitializer;

@Order(Ordered.HIGHEST_PRECEDENCE)
public class AppInfoInitializer implements WebApplicationInitializer {

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
		if (AppInfo.getHttpPort() == 0) {
			int port = ContainerDetector.port(servletContext);
			if (port > 0) {
				AppInfo.setHttpPort(port);
				logger.info("Server port auto detected: {}", port);
			}
		}
		logger.info("Default timezone {}", TimeZone.getDefault().getID());
		logger.info(
				"app.name={},app.version={},app.instanceid={},app.stage={},app.runlevel={},app.home={},hostname={},hostaddress={},profiles={}",
				AppInfo.getAppName(), AppInfo.getAppVersion(), AppInfo.getInstanceId(), AppInfo.getStage().toString(),
				AppInfo.getRunLevel().toString(), AppInfo.getAppHome(), AppInfo.getHostName(), AppInfo.getHostAddress(),
				defaultProfiles != null ? defaultProfiles : "default");
	}
}