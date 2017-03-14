package org.ironrhino.core.remoting.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.PreDestroy;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.event.InstanceLifecycleEvent;
import org.ironrhino.core.event.InstanceShutdownEvent;
import org.ironrhino.core.event.InstanceStartupEvent;
import org.ironrhino.core.remoting.ExportServicesEvent;
import org.ironrhino.core.remoting.Remoting;
import org.ironrhino.core.remoting.ServiceRegistry;
import org.ironrhino.core.struts.I18N;
import org.ironrhino.core.util.AppInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.ConfigurableWebApplicationContext;

public abstract class AbstractServiceRegistry implements ServiceRegistry {

	private static final String CLASS_NAME_SERVER = "org.ironrhino.core.remoting.server.HttpInvokerServer";
	private static final String CLASS_NAME_CLIENT = "org.ironrhino.core.remoting.client.HttpInvokerClient";
	private static final boolean IS_SERVER_PRESENT = ClassUtils.isPresent(CLASS_NAME_SERVER,
			AbstractServiceRegistry.class.getClassLoader());
	private static final boolean IS_CLIENT_PRESENT = ClassUtils.isPresent(CLASS_NAME_CLIENT,
			AbstractServiceRegistry.class.getClassLoader());

	protected Logger logger = LoggerFactory.getLogger(getClass());

	@Value("${serviceRegistry.useHttps:false}")
	private boolean useHttps;

	@Autowired
	private ConfigurableApplicationContext ctx;

	protected Map<String, List<String>> importedServiceCandidates = new ConcurrentHashMap<>();

	protected Map<String, Object> exportedServices = new HashMap<>();

	protected Map<String, String> exportedServiceDescriptions = new TreeMap<>();

	private String localHost;

	@Override
	public String getLocalHost() {
		return localHost;
	}

	@Override
	public Map<String, Object> getExportedServices() {
		return exportedServices;
	}

	public void init() {
		if (!useHttps)
			localHost = AppInfo.getAppName() + '@' + AppInfo.getHostAddress() + ':'
					+ (AppInfo.getHttpPort() > 0 ? AppInfo.getHttpPort() : DEFAULT_HTTP_PORT);
		else
			localHost = "https://" + AppInfo.getAppName() + '@' + AppInfo.getHostAddress() + ':'
					+ (AppInfo.getHttpsPort() > 0 ? AppInfo.getHttpsPort() : DEFAULT_HTTPS_PORT);
		if (ctx instanceof ConfigurableWebApplicationContext) {
			String ctxPath = ((ConfigurableWebApplicationContext) ctx).getServletContext().getContextPath();
			if (!ctxPath.isEmpty())
				localHost += ctxPath;
		}
		String[] beanNames = ctx.getBeanDefinitionNames();
		for (String beanName : beanNames) {
			BeanDefinition bd = ctx.getBeanFactory().getBeanDefinition(beanName);
			if (!bd.isSingleton() || bd.isAbstract())
				continue;
			String beanClassName = bd.getBeanClassName();
			if (beanClassName == null)
				continue;
			Class<?> clazz = null;
			try {
				clazz = Class.forName(beanClassName);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				continue;
			}
			if (beanClassName.startsWith("org.ironrhino.core.remoting.client.") && beanClassName.endsWith("Client")) {
				// remoting_client
				String serviceName = (String) bd.getPropertyValues().getPropertyValue("serviceInterface").getValue();
				if (IS_CLIENT_PRESENT)
					importedServiceCandidates.put(serviceName, new ArrayList<String>());
			} else {
				if (IS_SERVER_PRESENT)
					export(clazz, beanName, beanClassName);
			}
		}
		if (IS_SERVER_PRESENT) {
			for (String serviceName : exportedServices.keySet())
				register(serviceName);
		} else {
			logger.warn("No class [" + CLASS_NAME_SERVER + "] found, skip register services");
		}
		if (IS_CLIENT_PRESENT) {
			for (String serviceName : importedServiceCandidates.keySet())
				lookup(serviceName);
		} else {
			logger.warn("No class [" + CLASS_NAME_CLIENT + "] found, skip lookup services");
		}
		onReady();
	}

	private static String trimAppName(String host) {
		int i = host.indexOf('@');
		if (i < 0)
			return host;
		String s = host.substring(i + 1);
		i = host.indexOf("://");
		return i < 0 ? s : host.substring(0, i + 3) + s;
	}

	private void export(Class<?> clazz, String beanName, String beanClassName) {
		if (!clazz.isInterface()) {
			Remoting remoting = clazz.getAnnotation(Remoting.class);
			if (remoting != null) {
				Class<?>[] classes = remoting.value();
				if (classes.length == 0) {
					Class<?>[] interfaces = clazz.getInterfaces();
					if (interfaces.length > 0)
						classes = interfaces;
				}
				if (classes.length == 0) {
					logger.warn("@Remoting on concrete class [{}] must assign interfaces to export services",
							clazz.getName());
				} else {
					for (Class<?> inte : classes) {
						if (!inte.isInterface()) {
							logger.warn("class [{}] in @Remoting on class [{}] must be interface", inte.getName(),
									clazz.getName());
						} else if (!inte.isAssignableFrom(clazz)) {
							logger.warn(" class [{}] must implements interface [{}] in @Remoting", clazz.getName(),
									inte.getName());
						} else {
							String key = inte.getName() + ".exported";
							if ("false".equals(AppInfo.getApplicationContextProperties().getProperty(key))) {
								logger.info("skiped export service [{}] for bean [{}#{}]@{} because {}=false",
										inte.getName(), beanClassName, beanName, localHost, key);
							} else {
								exportedServices.put(inte.getName(), ctx.getBean(beanName));
								String description = remoting.description();
								if (StringUtils.isNotBlank(description))
									description = I18N.getText(description);
								exportedServiceDescriptions.put(inte.getName(), description);
								logger.info("exported service [{}] for bean [{}#{}]@{}", inte.getName(), beanClassName,
										beanName, localHost);
							}
						}
					}
				}
			}
			Class<?>[] interfaces = clazz.getInterfaces();
			if (interfaces != null) {
				for (Class<?> inte : interfaces) {
					export(inte, beanName, beanClassName);
				}
			}
		} else {
			Remoting remoting = clazz.getAnnotation(Remoting.class);
			if (remoting != null) {
				String key = clazz.getName() + ".exported";
				if ("false".equals(AppInfo.getApplicationContextProperties().getProperty(key))) {
					logger.info("skiped export service [{}] for bean [{}#{}]@{} because {}=false", clazz.getName(),
							beanClassName, beanName, localHost, key);
				} else {
					exportedServices.put(clazz.getName(), ctx.getBean(beanName));
					String description = remoting.description();
					if (StringUtils.isNotBlank(description))
						description = I18N.getText(description);
					exportedServiceDescriptions.put(clazz.getName(), description);
					logger.info("exported service [{}] for bean [{}#{}]@{}", clazz.getName(), beanClassName, beanName,
							localHost);
				}
			}
			for (Class<?> c : clazz.getInterfaces())
				export(c, beanName, beanClassName);
		}
	}

	@Override
	public void evict(String host) {
		for (Map.Entry<String, List<String>> entry : importedServiceCandidates.entrySet()) {
			Iterator<String> it = entry.getValue().iterator();
			while (it.hasNext()) {
				if (trimAppName(it.next()).equals(host))
					it.remove();
			}
		}
	}

	@Override
	public String discover(String serviceName) {
		List<String> hosts = importedServiceCandidates.get(serviceName);
		if (hosts != null && hosts.size() > 0) {
			String host = hosts.get(ThreadLocalRandom.current().nextInt(hosts.size()));
			onDiscover(serviceName, host);
			host = trimAppName(host);
			return host;
		} else {
			return null;
		}

	}

	protected void onDiscover(String serviceName, String host) {
		logger.info("discovered " + serviceName + " from " + host);
	}

	protected abstract void register(String serviceName);

	protected abstract void unregister(String serviceName);

	protected abstract void onReady();

	protected abstract void lookup(String serviceName);

	@PreDestroy
	public void destroy() {
		for (String serviceName : exportedServices.keySet())
			unregister(serviceName);
	}

	@EventListener
	public void onApplicationEvent(InstanceLifecycleEvent event) {
		if (handle(event))
			return;
		if (event instanceof InstanceStartupEvent && event.isLocal())
			init();
		else if (event instanceof InstanceShutdownEvent && !event.isLocal()) {
			String instanceId = event.getInstanceId();
			String host = instanceId.substring(instanceId.lastIndexOf('@') + 1);
			evict(host);
		} else if (event instanceof ExportServicesEvent) {
			if (event.isLocal())
				return;
			ExportServicesEvent ev = (ExportServicesEvent) event;
			String instanceId = event.getInstanceId();
			String host = instanceId.substring(instanceId.lastIndexOf('@') + 1);
			for (String serviceName : ev.getExportServices()) {
				List<String> hosts = importedServiceCandidates.get(serviceName);
				if (hosts != null && !hosts.contains(host))
					hosts.add(host);
			}
		}
	}

	protected boolean handle(InstanceLifecycleEvent event) {
		return false;
	}

}
