package org.ironrhino.core.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.TimeZone;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.AppInfo.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

public class AppInfoListener implements ServletContextListener {

	public static volatile ServletContext SERVLET_CONTEXT;

	private Logger logger;

	@Override
	public void contextInitialized(ServletContextEvent event) {
		if (SERVLET_CONTEXT == null)
			SERVLET_CONTEXT = event.getServletContext();
		Properties appProperties = getAppProperties();
		String defaultProfiles = null;
		if (StringUtils
				.isBlank(System
						.getProperty(AbstractEnvironment.DEFAULT_PROFILES_PROPERTY_NAME))) {
			defaultProfiles = System
					.getenv(AbstractEnvironment.DEFAULT_PROFILES_PROPERTY_NAME
							.replaceAll("\\.", "_").toUpperCase());
			if (StringUtils.isNotBlank(defaultProfiles)) {
				System.setProperty(
						AbstractEnvironment.DEFAULT_PROFILES_PROPERTY_NAME,
						defaultProfiles);
			} else {
				defaultProfiles = appProperties
						.getProperty(AbstractEnvironment.DEFAULT_PROFILES_PROPERTY_NAME);
				if (StringUtils.isNotBlank(defaultProfiles)) {
					System.setProperty(
							AbstractEnvironment.DEFAULT_PROFILES_PROPERTY_NAME,
							defaultProfiles);
				}
			}
		}
		String name = appProperties.getProperty(AppInfo.KEY_APP_NAME);
		if (StringUtils.isBlank(name))
			name = SERVLET_CONTEXT.getServletContextName();
		if (StringUtils.isNotBlank(name))
			AppInfo.setAppName(name);
		String version = appProperties.getProperty(AppInfo.KEY_APP_VERSION);
		if (StringUtils.isNotBlank(version))
			AppInfo.setAppVersion(version);
		String home = appProperties.getProperty(AppInfo.KEY_APP_HOME);
		if (StringUtils.isNotBlank(home))
			AppInfo.setAppHome(home);
		System.setProperty(AppInfo.KEY_APP_HOME, AppInfo.getAppHome());
		System.setProperty(AppInfo.KEY_APP_NAME, AppInfo.getAppName());
		System.setProperty("hibernate.logger.level",
				AppInfo.getStage() == Stage.DEVELOPMENT ? "TRACE" : "INFO");
		System.setProperty(
				"console.logger.level",
				AppInfo.getStage() == Stage.PRODUCTION
						&& (System.getProperty("os.name") == null || !System
								.getProperty("os.name").startsWith("Windows")) ? "ERROR"
						: "INFO");
		String context = SERVLET_CONTEXT.getRealPath("/");
		if (context == null)
			context = "";
		System.setProperty("app.context", context);

		String appBasePackage = appProperties
				.getProperty(AppInfo.KEY_APP_BASEPACKAGE);
		if (StringUtils.isBlank(appBasePackage))
			appBasePackage = "com." + AppInfo.getAppName();
		AppInfo.setAppBasePackage(appBasePackage);
		System.setProperty(AppInfo.KEY_APP_BASEPACKAGE, appBasePackage);
		String userTimezone = System.getProperty("user.timezone");
		if (StringUtils.isBlank(userTimezone)
				|| !TimeZone.getTimeZone(userTimezone).getID()
						.equals(userTimezone)) {
			userTimezone = "Asia/Shanghai";
			TimeZone older = TimeZone.getDefault();
			TimeZone newer = TimeZone.getTimeZone(userTimezone);
			if (!newer.getID().equals(older.getID()))
				TimeZone.setDefault(newer);
		}
		logger = LoggerFactory.getLogger(getClass());
		logger.info("default timezone {}", TimeZone.getDefault().getID());
		logger.info(
				"app.name={},app.version={},app.instanceid={},app.stage={},app.runlevel={},app.home={},hostname={},hostaddress={},profiles={}",
				AppInfo.getAppName(), AppInfo.getAppVersion(), AppInfo
						.getInstanceId(), AppInfo.getStage().toString(),
				AppInfo.getRunLevel().toString(), AppInfo.getAppHome(), AppInfo
						.getHostName(), AppInfo.getHostAddress(),
				defaultProfiles != null ? defaultProfiles : "default");
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		logger.info(
				"app.name={},app.version={},app.instanceid={},app.stage={},app.runlevel={},app.home={},hostname={},hostaddress={} is shutdown",
				AppInfo.getAppName(), AppInfo.getAppVersion(), AppInfo
						.getInstanceId(), AppInfo.getStage().toString(),
				AppInfo.getRunLevel().toString(), AppInfo.getAppHome(), AppInfo
						.getHostName(), AppInfo.getHostAddress());
		SERVLET_CONTEXT = null;
	}

	private Properties getAppProperties() {
		Properties properties = new Properties();
		Resource resource = new ClassPathResource("ironrhino.properties");
		if (resource.exists()) {
			try (InputStream is = resource.getInputStream()) {
				properties.load(is);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return properties;
	}

}
