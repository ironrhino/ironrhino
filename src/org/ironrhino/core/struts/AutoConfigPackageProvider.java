package org.ironrhino.core.struts;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.struts.result.AutoConfigResult;
import org.ironrhino.core.util.ClassScaner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.ClassUtils;

import com.opensymphony.xwork2.Action;
import com.opensymphony.xwork2.ObjectFactory;
import com.opensymphony.xwork2.config.Configuration;
import com.opensymphony.xwork2.config.ConfigurationException;
import com.opensymphony.xwork2.config.PackageProvider;
import com.opensymphony.xwork2.config.entities.ActionConfig;
import com.opensymphony.xwork2.config.entities.InterceptorMapping;
import com.opensymphony.xwork2.config.entities.PackageConfig;
import com.opensymphony.xwork2.config.entities.ResultConfig;
import com.opensymphony.xwork2.config.providers.InterceptorBuilder;
import com.opensymphony.xwork2.inject.Inject;
import com.opensymphony.xwork2.util.LocalizedTextUtil;

public class AutoConfigPackageProvider implements PackageProvider {

	public static final String GLOBAL_MESSAGES_PATTERN = "resources/i18n/**/*.properties";

	private static final Logger log = LoggerFactory
			.getLogger(AutoConfigPackageProvider.class);

	@Inject(value = "ironrhino.autoconfig.parent.package", required = false)
	private String parentPackage = "ironrhino-default";

	private String defaultActionClass = EntityAction.class.getName();

	@Inject(value = "ironrhino.autoconfig.packages", required = false)
	private String packageConfigInXml;

	private Configuration configuration;

	private boolean initialized = false;

	private PackageLoader packageLoader;

	@Inject
	private ObjectFactory objectFactory;

	protected Map<String, Set<String>> configPackages() {
		Map<String, Set<String>> packages = new HashMap<String, Set<String>>();
		Set<String> packagePrefixes = new HashSet<String>();
		for (String pck : ClassScaner.getAppPackages()) {
			int i = pck.indexOf('.');
			packagePrefixes.add(i > 0 ? pck.substring(0, i) : pck);
		}
		Set<Class<?>> packageInfos = new HashSet<Class<?>>();
		for (String packagePrefix : packagePrefixes)
			packageInfos.addAll(ClassScaner.scanAnnotatedPackage(packagePrefix,
					AutoConfig.class));
		for (Class<?> packageInfo : packageInfos) {
			String name = packageInfo.getName();
			String packageName = name.substring(0, name.lastIndexOf('.'));
			AutoConfig ac = packageInfo.getAnnotation(AutoConfig.class);
			if (ac != null) {
				log.info("Loading autoconfig from " + name);
				String defaultNamespace = ac.namespace();
				if (defaultNamespace.equals(""))
					defaultNamespace = "/"
							+ packageName.substring(packageName
									.lastIndexOf('.') + 1);
				Set<String> set = packages.get(ac.namespace());
				if (set == null) {
					set = new HashSet<String>();
					packages.put(defaultNamespace, set);
				}

				set.add(packageName);
			}
		}

		if (StringUtils.isNotBlank(packageConfigInXml)) {
			String[] array = packageConfigInXml.split(";");
			for (String s : array) {
				String[] arr = s.split(":");
				String defaultNamespace = "/";
				String packs;
				if (arr.length == 1) {
					packs = arr[0];
				} else {
					defaultNamespace = arr[0];
					packs = arr[1];
				}
				Set<String> set = packages.get(defaultNamespace);
				if (set == null) {
					set = new HashSet<String>();
					packages.put(defaultNamespace, set);
				}
				for (String p : packs.split("\\s*,\\s*")) {
					for (Map.Entry<String, Set<String>> entry : packages
							.entrySet()) {
						if (entry.getValue().contains(p)) {
							entry.getValue().remove(p);
							log.warn(
									"package {} have been overriden from {} to {} in struts.xml",
									p, entry.getKey(), defaultNamespace);
						}
					}
					set.add(p);
				}
			}
		}
		return packages;
	}

	@Override
	public void init(Configuration configuration) throws ConfigurationException {
		this.configuration = configuration;
		ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
		String searchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX
				+ GLOBAL_MESSAGES_PATTERN;
		Map<String, String> messageBunldes = new HashMap<String, String>();
		try {
			Resource[] resources = resourcePatternResolver
					.getResources(searchPath);
			for (Resource res : resources) {
				String name = res.getURI().toString();
				String source = name.substring(0,
						name.indexOf("/resources/i18n/"));
				name = name.substring(name.indexOf("resources/i18n/"));
				name = org.ironrhino.core.util.StringUtils.trimLocale(name
						.substring(0, name.lastIndexOf('.')));
				name = org.springframework.util.ClassUtils
						.convertResourcePathToClassName(name);
				if (messageBunldes.containsKey(name)
						&& !source.equals(messageBunldes.get(name))) {
					log.warn("Global messages " + name + " ignored from "
							+ source + ",will load from "
							+ messageBunldes.get(name));
				} else {
					messageBunldes.put(name, source);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		for (String name : messageBunldes.keySet()) {
			LocalizedTextUtil.addDefaultResourceBundle(name);
			log.info("Loading global messages from " + name);
		}
	}

	@Override
	public void loadPackages() throws ConfigurationException {
		Map<String, Set<String>> packages = configPackages();
		if (packages.size() == 0)
			return;
		for (Map.Entry<String, Set<String>> entry : packages.entrySet()) {
			String defaultNamespace = entry.getKey();
			Set<String> currentPackages = entry.getValue();
			Set<Class<?>> classes = ClassScaner.scanAnnotated(
					currentPackages.toArray(new String[0]), AutoConfig.class);
			if (classes.size() == 0)
				continue;
			packageLoader = new PackageLoader();
			for (Class<?> clazz : classes) {
				if (clazz.getSimpleName().equals("package-info"))
					continue;
				boolean inAnotherPackage = false;
				String thisPackage = clazz.getPackage().getName();
				String currentPackage = null;
				for (String s : currentPackages) {
					if ((thisPackage.startsWith(s + ".") || thisPackage
							.equals(s))
							&& (currentPackage == null || s.length() > currentPackage
									.length()))
						currentPackage = s;
				}
				String anotherPackage = null;
				for (String ns : packages.keySet()) {
					if (ns.equals(defaultNamespace))
						continue;
					Set<String> set = packages.get(ns);
					for (String s : set) {
						if ((thisPackage.startsWith(s + ".") || thisPackage
								.equals(s))
								&& (anotherPackage == null || s.length() > anotherPackage
										.length()))
							anotherPackage = s;
					}
				}
				if (anotherPackage != null
						&& anotherPackage.length() > currentPackage.length())
					inAnotherPackage = true;
				if (inAnotherPackage)
					continue;
				processAutoConfigClass(clazz, defaultNamespace);
			}
			for (PackageConfig packageConfig : packageLoader
					.createPackageConfigs()) {
				PackageConfig pc = configuration.getPackageConfig(packageConfig
						.getName());
				if (pc == null) {
					// add package
					configuration.addPackageConfig(packageConfig.getName(),
							packageConfig);
					for (ActionConfig ac : packageConfig.getActionConfigs()
							.values())
						log.info("mapping "
								+ ac.getClassName()
								+ " to "
								+ packageConfig.getNamespace()
								+ (packageConfig.getNamespace().endsWith("/") ? ""
										: "/") + ac.getName());
				} else {
					Map<String, ActionConfig> actionConfigs = new LinkedHashMap<String, ActionConfig>(
							pc.getActionConfigs());
					for (String actionName : packageConfig.getActionConfigs()
							.keySet()) {
						if (actionConfigs.containsKey(actionName)) {
							// ignore if action already exists
							log.warn(actionConfigs.get(actionName)
									+ " exists for action class '"
									+ actionConfigs.get(actionName)
											.getClassName()
									+ "',ignore autoconfig on action class '"
									+ packageConfig.getActionConfigs()
											.get(actionName).getClassName()
									+ "'");
							continue;
						}
						ActionConfig ac = packageConfig.getActionConfigs().get(
								actionName);
						actionConfigs.put(actionName, ac);
						log.info("mapping " + ac.getClassName() + " to "
								+ pc.getNamespace()
								+ (pc.getNamespace().endsWith("/") ? "" : "/")
								+ ac.getName());
					}
					// this is a trick
					try {
						Field field = PackageConfig.class
								.getDeclaredField("actionConfigs");
						field.setAccessible(true);
						field.set(pc, actionConfigs);
					} catch (Exception e) {
						log.error(e.getMessage(), e);
					}
				}

			}
		}
		initialized = true;
	}

	protected void processAutoConfigClass(Class<?> cls, String defaultNamespace) {
		AutoConfig ac = cls.getAnnotation(AutoConfig.class);
		String[] arr = getNamespaceAndActionName(cls, defaultNamespace);
		String namespace = arr[0];
		String actionName = arr[1];
		String actionClass = arr[2];
		String packageName;
		if (!"".equals(namespace)) {
			packageName = namespace.substring(1);
			packageName = packageName.replace('/', '_');
		} else {
			packageName = "default";
		}
		PackageConfig.Builder pkgConfig = loadPackageConfig(packageName);

		ResultConfig autoCofigResult = new ResultConfig.Builder("*",
				AutoConfigResult.class.getName()).build();
		ActionConfig.Builder builder = new ActionConfig.Builder(packageName,
				actionName, actionClass).addResultConfig(autoCofigResult);
		if (StringUtils.isNotBlank(ac.fileupload())) {
			Map<String, String> params = new HashMap<String, String>();
			params.put("allowedTypes", ac.fileupload());
			List<InterceptorMapping> interceptors = InterceptorBuilder
					.constructInterceptorReference(pkgConfig, "fileUpload",
							params, null, objectFactory);
			interceptors
					.addAll(InterceptorBuilder.constructInterceptorReference(
							pkgConfig, "annotationDefaultStack", null, null,
							objectFactory));
			builder.interceptors(interceptors);
		}
		ActionConfig actionConfig = builder.build();
		pkgConfig.addActionConfig(actionName, actionConfig);

	}

	protected PackageConfig.Builder loadPackageConfig(String packageName) {
		String namespace;
		if (packageName.equals("default"))
			namespace = "/";
		else
			namespace = "/" + packageName.replace('_', '/');
		PackageConfig.Builder pkgConfig = packageLoader.getPackage(packageName);
		if (pkgConfig == null) {
			pkgConfig = new PackageConfig.Builder(packageName);
			pkgConfig.namespace(namespace);
			PackageConfig parent = configuration
					.getPackageConfig(parentPackage);
			if (parent != null) {
				pkgConfig.addParent(parent);
			} else {
				log.error("Unable to locate parent package: " + parentPackage);
			}
			packageLoader.registerPackage(pkgConfig);
		} else if (pkgConfig.getNamespace() == null) {
			pkgConfig.namespace(namespace);
		}
		return pkgConfig;
	}

	@Override
	public boolean needsReload() {
		return !initialized;
	}

	private static class PackageLoader {

		private Map<String, PackageConfig.Builder> packageConfigBuilders = new HashMap<String, PackageConfig.Builder>();

		private Map<PackageConfig.Builder, PackageConfig.Builder> childToParent = new HashMap<PackageConfig.Builder, PackageConfig.Builder>();

		public PackageConfig.Builder getPackage(String name) {
			return packageConfigBuilders.get(name);
		}

		public void registerPackage(PackageConfig.Builder builder) {
			packageConfigBuilders.put(builder.getName(), builder);
		}

		public Collection<PackageConfig> createPackageConfigs() {
			Map<String, PackageConfig> configs = new HashMap<String, PackageConfig>();

			Set<PackageConfig.Builder> builders;
			while ((builders = findPackagesWithNoParents()).size() > 0) {
				for (PackageConfig.Builder parent : builders) {
					PackageConfig config = parent.build();
					configs.put(config.getName(), config);
					packageConfigBuilders.remove(config.getName());

					for (Iterator<Map.Entry<PackageConfig.Builder, PackageConfig.Builder>> i = childToParent
							.entrySet().iterator(); i.hasNext();) {
						Map.Entry<PackageConfig.Builder, PackageConfig.Builder> entry = i
								.next();
						if (entry.getValue() == parent) {
							entry.getKey().addParent(config);
							i.remove();
						}
					}
				}
			}
			return configs.values();
		}

		Set<PackageConfig.Builder> findPackagesWithNoParents() {
			Set<PackageConfig.Builder> builders = new HashSet<PackageConfig.Builder>();
			for (PackageConfig.Builder child : packageConfigBuilders.values()) {
				if (!childToParent.containsKey(child)) {
					builders.add(child);
				}
			}
			return builders;
		}

	}

	private static Map<String, Class<?>> entityClassURLMapping = new ConcurrentHashMap<String, Class<?>>();

	public String[] getNamespaceAndActionName(Class<?> cls,
			String defaultNamespace) {
		String actionName = null;
		String namespace = null;
		String actionClass = null;
		AutoConfig ac = cls.getAnnotation(AutoConfig.class);
		if (Persistable.class.isAssignableFrom(cls)) {
			actionName = StringUtils.uncapitalize(cls.getSimpleName());
			namespace = ac.namespace();
			if (StringUtils.isBlank(namespace))
				namespace = defaultNamespace;
			actionClass = cls.getName().replace("model", "action") + "Action";
			if (!ClassUtils.isPresent(actionClass, getClass().getClassLoader()))
				actionClass = defaultActionClass;
			entityClassURLMapping.put(namespace
					+ (namespace.endsWith("/") ? "" : "/") + actionName, cls);
		} else if (Action.class.isAssignableFrom(cls)) {
			actionName = StringUtils.uncapitalize(cls.getSimpleName());
			if (actionName.endsWith("Action"))
				actionName = actionName.substring(0, actionName.length() - 6);
			actionClass = cls.getName();
			namespace = ac.namespace();
			if (StringUtils.isBlank(namespace))
				namespace = defaultNamespace;
		}
		if (namespace == null)
			namespace = "";
		if (!ac.actionName().equals(""))
			actionName = ac.actionName();
		return new String[] { namespace, actionName, actionClass };
	}

	public static Class<?> getEntityClass(String namespace, String actionName) {
		return entityClassURLMapping.get(namespace
				+ (namespace.endsWith("/") ? "" : "/") + actionName);
	}

	public static String getEntityUrl(Class<?> entityClass) {
		for (Map.Entry<String, Class<?>> entry : entityClassURLMapping
				.entrySet())
			if (entry.getValue().equals(entityClass))
				return entry.getKey();
		return null;
	}

	public Collection<PackageConfig> getAllPackageConfigs() {
		return configuration.getPackageConfigs().values();
	}

}