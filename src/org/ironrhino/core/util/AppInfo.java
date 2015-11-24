package org.ironrhino.core.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

public class AppInfo {

	public static final String KEY_STAGE = "STAGE";

	public static final String KEY_RUNLEVEL = "RUNLEVEL";

	public static final String KEY_APP_NAME = "app.name";

	public static final String KEY_APP_HOME = "app.home";

	public static final String KEY_APP_BASEPACKAGE = "app.basePackage";

	public static final String KEY_APP_EXCLUDEFILTERREGEX = "app.excludeFilterRegex";

	public static final String KEY_APP_VERSION = "app.version";

	public static final String KEY_RACK = "RACK";

	public static final String DEFAULT_RACK = "/default-rack";

	private static String name = "app";

	private static String _instanceId;

	private static String home;

	private static String basePackage;

	private static String excludeFilterRegex;

	private static String version = "1.0.0";

	private static int httpPort = 0;

	private static final Stage STAGE;

	private static final RunLevel RUNLEVEL;

	private static final String HOSTNAME;

	private static final String HOSTADDRESS;

	private static final String NODEPATH;

	static {
		_instanceId = CodecUtils.nextId().substring(0, 10);
		String stage = getEnv(KEY_STAGE);
		Stage s = null;
		if (stage != null)
			try {
				s = Stage.valueOf(stage.toUpperCase());
			} catch (Exception e) {
			}
		if (s != null)
			STAGE = s;
		else
			STAGE = Stage.PRODUCTION;
		String runlevel = getEnv(KEY_RUNLEVEL);
		RunLevel r = null;
		if (runlevel != null)
			try {
				r = RunLevel.valueOf(runlevel.toUpperCase());
			} catch (Exception e) {
			}
		if (r != null)
			RUNLEVEL = r;
		else
			RUNLEVEL = RunLevel.NORMAL;
		String name = null;
		String address = "127.0.0.1";
		try {
			InetAddress[] addresses = InetAddress.getAllByName(InetAddress.getLocalHost().getHostName());
			for (InetAddress addr : addresses) {
				String ip = addr.getHostAddress();
				if (ip.split("\\.").length != 4 || ip.startsWith("169.254."))
					continue;
				name = addr.getHostName();
				address = ip;
				break;
			}
			if (name == null) {
				InetAddress addr = InetAddress.getLocalHost();
				name = addr.getHostName();
			}
		} catch (UnknownHostException e) {
			name = "unknown";
		}
		HOSTNAME = name;
		HOSTADDRESS = address;

		String p = System.getProperty("port.http");
		if (StringUtils.isBlank(p))
			p = System.getProperty("port.http.nonssl");
		if (StringUtils.isBlank(p))
			p = System.getenv("VCAP_APP_PORT");
		if (StringUtils.isNotBlank(p) && StringUtils.isNumeric(p))
			httpPort = Integer.valueOf(p);

		String rack = getEnv(KEY_RACK);
		if (rack == null)
			rack = DEFAULT_RACK;
		StringBuilder sb = new StringBuilder();
		if (!rack.startsWith("/"))
			sb.append("/");
		sb.append(rack);
		if (!rack.endsWith("/"))
			sb.append("/");
		sb.append(HOSTNAME);
		NODEPATH = sb.toString();
	}

	public static void setAppName(String name) {
		AppInfo.name = name;
	}

	public static void setAppHome(String home) {
		AppInfo.home = home;
	}

	public static void setAppBasePackage(String basePackage) {
		AppInfo.basePackage = basePackage;
	}

	public static void setExcludeFilterRegex(String excludeFilterRegex) {
		AppInfo.excludeFilterRegex = excludeFilterRegex;
	}

	public static void setAppVersion(String version) {
		AppInfo.version = version;
	}

	public static Stage getStage() {
		return STAGE;
	}

	public static RunLevel getRunLevel() {
		return RUNLEVEL;
	}

	public static String getAppName() {
		return name;
	}

	public static String getInstanceId() {
		StringBuilder sb = new StringBuilder();
		sb.append(getAppName()).append("-").append(_instanceId).append("@").append(getHostAddress());
		int httpPort = getHttpPort();
		if (httpPort > 0)
			sb.append(':').append(httpPort);
		return sb.toString();
	}

	public static String getAppVersion() {
		return version;
	}

	public static String getAppHome() {
		if (home == null) {
			String userhome = System.getProperty("user.home");
			if (userhome.indexOf("nonexistent") >= 0) // for cloudfoundry
				userhome = System.getenv().get("HOME");
			home = userhome.replace('\\', File.separatorChar) + File.separator + name;
		}
		return home;
	}

	public static String getAppBasePackage() {
		return basePackage;
	}

	public static String getExcludeFilterRegex() {
		return excludeFilterRegex;
	}

	public static String getHostName() {
		return HOSTNAME;
	}

	public static String getHostAddress() {
		return HOSTADDRESS;
	}

	public static int getHttpPort() {
		return httpPort;
	}

	public static String getNodePath() {
		return NODEPATH;
	}

	public static void initialize() {
		Properties appProperties = new Properties();
		Resource resource = new ClassPathResource("ironrhino.properties");
		if (resource.exists()) {
			try (InputStream is = resource.getInputStream()) {
				appProperties.load(is);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		String name = appProperties.getProperty(AppInfo.KEY_APP_NAME);
		if (StringUtils.isNotBlank(name))
			AppInfo.setAppName(name);
		String version = appProperties.getProperty(AppInfo.KEY_APP_VERSION);
		if (StringUtils.isNotBlank(version))
			AppInfo.setAppVersion(version);
		String home = appProperties.getProperty(AppInfo.KEY_APP_HOME);
		if (StringUtils.isNotBlank(home))
			AppInfo.setAppHome(home);
		System.setProperty(AppInfo.KEY_STAGE, AppInfo.getStage().name());
		System.setProperty(AppInfo.KEY_APP_HOME, AppInfo.getAppHome());
		System.setProperty(AppInfo.KEY_APP_NAME, AppInfo.getAppName());
		String appBasePackage = appProperties.getProperty(AppInfo.KEY_APP_BASEPACKAGE);
		if (StringUtils.isBlank(appBasePackage))
			appBasePackage = "com." + AppInfo.getAppName().replaceAll("-", ".");
		AppInfo.setAppBasePackage(appBasePackage);
		System.setProperty(AppInfo.KEY_APP_BASEPACKAGE, appBasePackage);
		String excludeFilterRegex = appProperties.getProperty(AppInfo.KEY_APP_EXCLUDEFILTERREGEX);
		if (StringUtils.isNotBlank(excludeFilterRegex)) {
			AppInfo.setExcludeFilterRegex(excludeFilterRegex);
			System.setProperty(AppInfo.KEY_APP_EXCLUDEFILTERREGEX, excludeFilterRegex);
		}

		// configure spring profiles
		if (StringUtils.isBlank(System.getProperty(AbstractEnvironment.DEFAULT_PROFILES_PROPERTY_NAME))) {
			String defaultProfiles = System
					.getenv(AbstractEnvironment.DEFAULT_PROFILES_PROPERTY_NAME.replaceAll("\\.", "_").toUpperCase());
			if (StringUtils.isNotBlank(defaultProfiles)) {
				System.setProperty(AbstractEnvironment.DEFAULT_PROFILES_PROPERTY_NAME, defaultProfiles);
			} else {
				defaultProfiles = appProperties.getProperty(AbstractEnvironment.DEFAULT_PROFILES_PROPERTY_NAME);
				if (StringUtils.isNotBlank(defaultProfiles)) {
					System.setProperty(AbstractEnvironment.DEFAULT_PROFILES_PROPERTY_NAME, defaultProfiles);
				}
			}
		}

		// configure log4j2
		System.setProperty("Log4jContextSelector", "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector");
		if (System.getProperty("AsyncLogger.RingBufferSize") == null)
			System.setProperty("AsyncLogger.RingBufferSize", "16384");
		System.setProperty("hibernate.logger.level", AppInfo.getStage() == Stage.DEVELOPMENT ? "TRACE" : "INFO");
		System.setProperty("console.logger.level", AppInfo.getStage() == Stage.PRODUCTION
				&& (System.getProperty("os.name") == null || !System.getProperty("os.name").startsWith("Windows"))
						? "ERROR" : "INFO");

		// configure timezone
		String userTimezone = System.getProperty("user.timezone");
		if (StringUtils.isBlank(userTimezone) || !TimeZone.getTimeZone(userTimezone).getID().equals(userTimezone)) {
			userTimezone = "Asia/Shanghai";
			TimeZone older = TimeZone.getDefault();
			TimeZone newer = TimeZone.getTimeZone(userTimezone);
			if (!newer.getID().equals(older.getID()))
				TimeZone.setDefault(newer);
		}
	}

	private static Properties applicationContextProperties = null;

	public static Properties getApplicationContextProperties() {
		if (applicationContextProperties == null) {
			Properties properties = new Properties();
			Resource resource = new ClassPathResource("resources/spring/applicationContext.properties");
			if (resource.exists()) {
				try (InputStream is = resource.getInputStream()) {
					properties.load(is);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			resource = new ClassPathResource(
					"resources/spring/applicationContext." + AppInfo.getStage().name() + ".properties");
			if (resource.exists()) {
				try (InputStream is = resource.getInputStream()) {
					properties.load(is);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			resource = new FileSystemResource(AppInfo.getAppHome() + "/conf/applicationContext.properties");
			if (resource.exists()) {
				try (InputStream is = resource.getInputStream()) {
					properties.load(is);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			resource = new FileSystemResource(
					AppInfo.getAppHome() + "/conf/applicationContext." + AppInfo.getStage().name() + ".properties");
			if (resource.exists()) {
				try (InputStream is = resource.getInputStream()) {
					properties.load(is);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			applicationContextProperties = properties;
		}
		return applicationContextProperties;
	}

	private static String getEnv(String key) {
		String value = System.getProperty(key);
		if (value == null)
			value = System.getenv(key);
		return value;
	}

	public static enum Stage {
		DEVELOPMENT, TEST, PREPARATION, PRODUCTION
	}

	public static enum RunLevel {
		LOW, NORMAL, HIGH
	}

}
