package org.ironrhino.core.servlet;

import java.io.IOException;
import java.util.TimeZone;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

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