package org.ironrhino.core.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.ironrhino.core.log4j.SimpleMergeStrategy;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import lombok.experimental.UtilityClass;

@UtilityClass
public class AppInfo {

	public static final String KEY_STAGE = "STAGE";

	public static final String KEY_RUNLEVEL = "RUNLEVEL";

	public static final String KEY_APP_NAME = "app.name";

	public static final String KEY_APP_HOME = "app.home";

	public static final String KEY_APP_BASEPACKAGE = "app.basePackage";

	public static final String KEY_APP_EXCLUDEFILTERREGEX = "app.excludeFilterRegex";

	public static final String KEY_APP_FEATUREPROFILES = "app.featureProfiles";

	public static final String KEY_APP_VERSION = "app.version";

	public static final String KEY_APP_INSTANCEID = "app.instanceId";

	public static final String KEY_RACK = "RACK";

	public static final String DEFAULT_RACK = "/default-rack";

	private static String name = "app";

	private static String _instanceId;

	private static String home;

	private static String basePackage;

	private static String excludeFilterRegex;

	private static String defaultProfiles;

	private static String featureProfiles;

	private static String version = "1.0.0";

	private static String serverInfo;

	private static int httpPort = 0;

	private static int httpsPort = 0;

	private static String contextPath = null;

	private static final Stage STAGE;

	private static final RunLevel RUNLEVEL;

	private static final String HOSTNAME;

	private static final String HOSTADDRESS;

	private static final String NODEPATH;

	private static final String IRONRHINO_VERSION;

	static {
		String temp = CodecUtils.nextId();
		_instanceId = temp.substring(temp.length() - 10, temp.length());
		String stage = getEnv(KEY_STAGE);
		Stage s = null;
		if (stage != null)
			try {
				s = Stage.valueOf(stage.toUpperCase(Locale.ROOT));
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
				r = RunLevel.valueOf(runlevel.toUpperCase(Locale.ROOT));
			} catch (Exception e) {
			}
		if (r != null)
			RUNLEVEL = r;
		else
			RUNLEVEL = RunLevel.NORMAL;
		String name = System.getProperty("host.name");
		String address = System.getProperty("host.address");
		if (address == null)
			try {
				Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
				loop: while (e.hasMoreElements()) {
					NetworkInterface n = e.nextElement();
					Enumeration<InetAddress> ee = n.getInetAddresses();
					while (ee.hasMoreElements()) {
						InetAddress addr = ee.nextElement();
						String ip = addr.getHostAddress();
						if (ip.equals("127.0.0.1") || ip.split("\\.").length != 4 || ip.startsWith("169.254."))
							continue;
						address = ip;
						break loop;
					}
				}
			} catch (SocketException e) {
			}
		try {
			if (name == null)
				name = InetAddress.getLocalHost().getHostName();
			if (address == null) {
				InetAddress[] addresses = InetAddress.getAllByName(name);
				for (InetAddress addr : addresses) {
					String ip = addr.getHostAddress();
					if (ip.equals("127.0.0.1") || ip.split("\\.").length != 4 || ip.startsWith("169.254."))
						continue;
					address = ip;
					break;
				}
			}
		} catch (UnknownHostException ex) {
			name = "unknown";
		}
		HOSTNAME = name;
		HOSTADDRESS = address != null ? address : "127.0.0.1";

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

		Properties appProperties = new Properties();
		Resource resource = new ClassPathResource("ironrhino.properties");
		if (resource.exists()) {
			try (InputStream is = resource.getInputStream()) {
				appProperties.load(is);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		String appName = appProperties.getProperty(KEY_APP_NAME);
		if (StringUtils.isNotBlank(appName))
			setAppName(appName);
		String version = appProperties.getProperty(KEY_APP_VERSION);
		if (StringUtils.isNotBlank(version))
			setAppVersion(version);
		String home = appProperties.getProperty(KEY_APP_HOME);
		if (StringUtils.isNotBlank(home))
			setAppHome(home);
		String appBasePackage = appProperties.getProperty(KEY_APP_BASEPACKAGE);
		if (StringUtils.isBlank(appBasePackage))
			appBasePackage = "com." + getAppName().replaceAll("-", ".");
		setAppBasePackage(appBasePackage);
		String excludeFilterRegex = appProperties.getProperty(KEY_APP_EXCLUDEFILTERREGEX);
		if (StringUtils.isNotBlank(excludeFilterRegex))
			setExcludeFilterRegex(excludeFilterRegex);
		String defaultProfiles = appProperties.getProperty(AbstractEnvironment.DEFAULT_PROFILES_PROPERTY_NAME);
		if (StringUtils.isNotBlank(defaultProfiles))
			setDefaultProfiles(defaultProfiles);
		String featureProfiles = appProperties.getProperty(KEY_APP_FEATUREPROFILES);
		if (StringUtils.isNotBlank(featureProfiles))
			setFeatureProfiles(featureProfiles);

		Package pkg = AppInfo.class.getPackage();
		if (pkg != null && "ironrhino".equals(pkg.getImplementationVendor())
				&& "ironrhino-core".equals(pkg.getImplementationTitle())) {
			IRONRHINO_VERSION = pkg.getImplementationVersion();
		} else {
			IRONRHINO_VERSION = null;
		}
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

	public static void setDefaultProfiles(String defaultProfiles) {
		AppInfo.defaultProfiles = defaultProfiles;
	}

	public static void setFeatureProfiles(String featureProfiles) {
		AppInfo.featureProfiles = featureProfiles;
	}

	public static void setAppVersion(String version) {
		AppInfo.version = version;
	}

	public static void setServerInfo(String serverInfo) {
		AppInfo.serverInfo = serverInfo;
	}

	public static void setHttpPort(int httpPort) {
		AppInfo.httpPort = httpPort;
	}

	public static void setHttpsPort(int httpsPort) {
		AppInfo.httpsPort = httpsPort;
	}

	public static void setContextPath(String contextPath) {
		AppInfo.contextPath = contextPath;
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
		return getInstanceId(false);
	}

	public static String getInstanceId(boolean lenient) {
		return getInstanceId(lenient, false);
	}

	public static String getInstanceId(boolean lenient, boolean excludeAppName) {
		StringBuilder sb = new StringBuilder();
		if (!excludeAppName) {
			sb.append(getAppName());
			if (!lenient)
				sb.append("-").append(_instanceId);
			sb.append("@");
		} else if (!lenient) {
			sb.append(_instanceId);
			sb.append("@");
		}
		sb.append(getHostAddress());
		int httpPort = getHttpPort();
		if (httpPort > 0)
			sb.append(':').append(httpPort);
		String contextPath = getContextPath();
		if (StringUtils.isNotBlank(contextPath))
			sb.append(contextPath);
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

	public static String getDefaultProfiles() {
		return defaultProfiles;
	}

	public static String getFeatureProfiles() {
		return featureProfiles;
	}

	public static String getHostName() {
		return HOSTNAME;
	}

	public static String getHostAddress() {
		return HOSTADDRESS;
	}

	public static String getIronrhinoVersion() {
		return IRONRHINO_VERSION;
	}

	public static String getServerInfo() {
		return serverInfo;
	}

	public static int getHttpPort() {
		return httpPort;
	}

	public static int getHttpsPort() {
		return httpsPort;
	}

	public static String getContextPath() {
		return contextPath;
	}

	public static String getNodePath() {
		return NODEPATH;
	}

	public static void initialize() {

		if (System.getProperty("http.maxConnections") == null)
			System.setProperty("http.maxConnections", "100");
		System.setProperty("sun.net.http.errorstream.enableBuffering", "true");

		String p = System.getProperty("port.http");
		if (StringUtils.isBlank(p))
			p = System.getProperty("port.http.nonssl");
		if (StringUtils.isBlank(p))
			p = System.getenv("VCAP_APP_PORT");
		if (StringUtils.isNotBlank(p) && StringUtils.isNumeric(p))
			httpPort = Integer.valueOf(p);
		p = System.getProperty("port.https");
		if (StringUtils.isBlank(p))
			p = System.getProperty("port.http.ssl");
		if (StringUtils.isNotBlank(p) && StringUtils.isNumeric(p))
			httpsPort = Integer.valueOf(p);

		System.setProperty(AppInfo.KEY_STAGE, AppInfo.getStage().name());
		System.setProperty(AppInfo.KEY_APP_HOME, AppInfo.getAppHome());
		System.setProperty(AppInfo.KEY_APP_NAME, AppInfo.getAppName());
		System.setProperty(AppInfo.KEY_APP_BASEPACKAGE, AppInfo.getAppBasePackage());
		String excludeFilterRegex = AppInfo.getExcludeFilterRegex();
		if (StringUtils.isNotBlank(excludeFilterRegex))
			System.setProperty(AppInfo.KEY_APP_EXCLUDEFILTERREGEX, excludeFilterRegex);

		// configure spring profiles
		if (StringUtils.isBlank(System.getProperty(AbstractEnvironment.DEFAULT_PROFILES_PROPERTY_NAME))) {
			String defaultProfiles = System.getenv(
					AbstractEnvironment.DEFAULT_PROFILES_PROPERTY_NAME.replaceAll("\\.", "_").toUpperCase(Locale.ROOT));
			if (StringUtils.isNotBlank(defaultProfiles)) {
				System.setProperty(AbstractEnvironment.DEFAULT_PROFILES_PROPERTY_NAME, defaultProfiles);
				AppInfo.setDefaultProfiles(defaultProfiles);
			} else {
				defaultProfiles = AppInfo.getDefaultProfiles();
				if (StringUtils.isNotBlank(defaultProfiles)) {
					System.setProperty(AbstractEnvironment.DEFAULT_PROFILES_PROPERTY_NAME, defaultProfiles);
				}
			}
		}
		String featureProfiles = getFeatureProfiles();
		if (StringUtils.isNotBlank(featureProfiles)) {
			String defaultProfiles = System.getProperty(AbstractEnvironment.DEFAULT_PROFILES_PROPERTY_NAME);
			if (StringUtils.isBlank(defaultProfiles))
				defaultProfiles = "default";
			defaultProfiles = defaultProfiles + ',' + featureProfiles;
			System.setProperty(AbstractEnvironment.DEFAULT_PROFILES_PROPERTY_NAME,
					String.join(",", new TreeSet<>(Arrays.asList(defaultProfiles.split("\\s*,\\s*")))));
		}

		// configure log4j2
		StringBuilder sb = new StringBuilder("classpath:log4j2.xml");
		for (String file : new String[] { "log4j2." + AppInfo.getStage().name() + ".xml", "log4j2-app.xml",
				"log4j2-app." + AppInfo.getStage().name() + ".xml" })
			if (AppInfo.class.getClassLoader().getResource(file) != null)
				sb.append(",classpath:" + file);
		String configurationFile = sb.toString();
		System.setProperty("log4j.configurationFile", configurationFile);
		if (configurationFile.indexOf(',') > 0)
			System.setProperty("log4j.mergeStrategy", SimpleMergeStrategy.class.getName());
		if (!SystemUtils.IS_OS_WINDOWS) {
			if (System.getProperty("Log4jContextSelector") == null)
				System.setProperty("Log4jContextSelector",
						"org.apache.logging.log4j.core.async.AsyncLoggerContextSelector");
			if (System.getProperty("AsyncLogger.RingBufferSize") == null)
				System.setProperty("AsyncLogger.RingBufferSize", "16384");
		}
		if (System.getProperty("hibernate.logger.level") == null)
			System.setProperty("hibernate.logger.level", AppInfo.getStage() == Stage.DEVELOPMENT ? "TRACE" : "INFO");
		if (System.getProperty("console.logger.level") == null)
			System.setProperty("console.logger.level", AppInfo.getStage() == Stage.PRODUCTION
					&& (System.getProperty("os.name") == null || !System.getProperty("os.name").startsWith("Windows"))
							? "ERROR"
							: "INFO");
		String kafkaBootstrapServers = getRawApplicationContextProperties().getProperty("kafka.bootstrap.servers");
		if (StringUtils.isNotBlank(kafkaBootstrapServers))
			System.setProperty("kafka.bootstrap.servers", kafkaBootstrapServers);

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

	private static Properties getRawApplicationContextProperties() {
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
		return properties;
	}

	public static Properties getApplicationContextProperties() {
		if (applicationContextProperties == null) {
			Properties properties = getRawApplicationContextProperties();
			Properties temp = new Properties();
			MutablePropertySources propertySources = new MutablePropertySources();
			PropertySource<?> localPropertySource = new PropertiesPropertySource("local", properties);
			propertySources.addFirst(localPropertySource);
			PropertySourcesPropertyResolver propertyResolver = new PropertySourcesPropertyResolver(propertySources);
			for (String key : properties.stringPropertyNames()) {
				String value = properties.getProperty(key);
				value = propertyResolver.resolvePlaceholders(value);
				temp.setProperty(key, value);
			}
			applicationContextProperties = temp;
		}
		return applicationContextProperties;
	}

	public static String resolvePlaceholders(String text) {
		MutablePropertySources propertySources = new MutablePropertySources();
		PropertySource<?> localPropertySource = new PropertiesPropertySource("local",
				getApplicationContextProperties());
		propertySources.addFirst(localPropertySource);
		PropertySourcesPropertyResolver propertyResolver = new PropertySourcesPropertyResolver(propertySources);
		return propertyResolver.resolveRequiredPlaceholders(text);
	}

	private static String getEnv(String key) {
		String value = System.getProperty(key);
		if (value == null)
			value = System.getenv(key);
		return value;
	}

	public static enum Stage {
		DEVELOPMENT, TEST, PREPARATION, PRODUCTION, SANDBOX
	}

	public static enum RunLevel {
		LOW, NORMAL, HIGH
	}

}
