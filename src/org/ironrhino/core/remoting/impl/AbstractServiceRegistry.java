package org.ironrhino.core.remoting.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.PreDestroy;
import javax.servlet.ServletContext;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.remoting.DistanceMeasurer;
import org.ironrhino.core.remoting.Remoting;
import org.ironrhino.core.remoting.ServiceNotFoundException;
import org.ironrhino.core.remoting.ServiceRegistry;
import org.ironrhino.core.struts.I18N;
import org.ironrhino.core.util.AppInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.ConfigurableWebApplicationContext;

import lombok.Getter;

public abstract class AbstractServiceRegistry implements ServiceRegistry {

	private static final String CLASS_NAME_SERVER = "org.ironrhino.core.remoting.server.HttpInvokerServer";
	private static final String CLASS_NAME_CLIENT = "org.ironrhino.core.remoting.client.HttpInvokerClient";
	private static final boolean IS_SERVER_PRESENT = ClassUtils.isPresent(CLASS_NAME_SERVER,
			AbstractServiceRegistry.class.getClassLoader());
	private static final boolean IS_CLIENT_PRESENT = ClassUtils.isPresent(CLASS_NAME_CLIENT,
			AbstractServiceRegistry.class.getClassLoader());

	protected Logger logger = LoggerFactory.getLogger(getClass());

	@Value("${serviceRegistry.skipExport:false}")
	private boolean skipExport;

	@Value("${serviceRegistry.useHttps:false}")
	private boolean useHttps;

	@Value("${serviceRegistry.lbNodesThreshold:8}")
	private int lbNodesThreshold = 8;

	@Autowired(required = false)
	private DistanceMeasurer distanceMeasurer = DistanceMeasurer.DEFAULT;

	@Autowired
	private ConfigurableApplicationContext ctx;

	@Getter
	protected Map<String, List<String>> importedServiceCandidates = new ConcurrentHashMap<>();

	@Getter
	protected Map<String, Object> exportedServices = new HashMap<>();

	protected Map<String, String> exportedServiceDescriptions = new TreeMap<>();

	protected Map<String, String> importedServices = new ConcurrentHashMap<>();

	protected boolean ready;

	@Getter
	private String localHost;

	public void init() {
		if (!useHttps)
			localHost = AppInfo.getAppName() + '@' + AppInfo.getHostAddress() + ':'
					+ (AppInfo.getHttpPort() > 0 ? AppInfo.getHttpPort() : DEFAULT_HTTP_PORT);
		else
			localHost = "https://" + AppInfo.getAppName() + '@' + AppInfo.getHostAddress() + ':'
					+ (AppInfo.getHttpsPort() > 0 ? AppInfo.getHttpsPort() : DEFAULT_HTTPS_PORT);
		if (ctx instanceof ConfigurableWebApplicationContext) {
			ServletContext servletContext = ((ConfigurableWebApplicationContext) ctx).getServletContext();
			String ctxPath = servletContext != null ? servletContext.getContextPath() : "";
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
			if (beanClassName.equals(CLASS_NAME_CLIENT)) {
				// remoting_client
				PropertyValue pv = bd.getPropertyValues().getPropertyValue("serviceInterface");
				if (pv == null)
					continue;
				String serviceName = (String) pv.getValue();
				if (IS_CLIENT_PRESENT)
					importedServiceCandidates.put(serviceName, new CopyOnWriteArrayList<String>());
			} else {
				if (clazz != null && FactoryBean.class.isAssignableFrom(clazz) && bd instanceof RootBeanDefinition) {
					clazz = ((RootBeanDefinition) bd).getTargetType();
					if (clazz == null)
						continue;
					beanClassName = clazz.getName();
				}
				if (IS_SERVER_PRESENT && !skipExport)
					export(clazz, beanName, beanClassName);
			}
		}
		if (IS_SERVER_PRESENT) {
			for (String serviceName : exportedServices.keySet())
				register(serviceName);
		} else {
			logger.warn("No class [" + CLASS_NAME_SERVER + "] found, skipped register services");
		}
		if (IS_CLIENT_PRESENT) {
			for (String serviceName : importedServiceCandidates.keySet())
				lookup(serviceName);
		} else {
			logger.warn("No class [" + CLASS_NAME_CLIENT + "] found, skipped lookup services");
		}
		onReady();
	}

	private static String normalizeHost(String host) {
		int i = host.indexOf('@');
		if (i < 0)
			return host;
		String s = host.substring(i + 1);
		i = host.indexOf("://");
		return i < 0 ? s : host.substring(0, i + 3) + s;
	}

	private void export(Class<?> clazz, String beanName, String beanClassName) {
		if (!clazz.isInterface()) {
			Remoting remoting = AnnotatedElementUtils.getMergedAnnotation(clazz, Remoting.class);
			if (remoting != null) {
				Class<?>[] classes = remoting.serviceInterfaces();
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
							logger.warn("Class [{}] in @Remoting on class [{}] must be interface", inte.getName(),
									clazz.getName());
						} else if (!inte.isAssignableFrom(clazz)) {
							logger.warn("Class [{}] must implements interface [{}] in @Remoting", clazz.getName(),
									inte.getName());
						} else {
							String key = inte.getName() + ".exported";
							if ("false".equals(AppInfo.getApplicationContextProperties().getProperty(key))) {
								logger.info("Skipped export service [{}] for bean [{}#{}]@{} because {}=false",
										inte.getName(), beanClassName, beanName, normalizeHost(localHost), key);
							} else {
								exportedServices.put(inte.getName(), ctx.getBean(beanName));
								String description = remoting.description();
								if (StringUtils.isNotBlank(description))
									description = I18N.getText(description);
								exportedServiceDescriptions.put(inte.getName(), description);
								logger.info("Exported service [{}] for bean [{}#{}]@{}", inte.getName(), beanClassName,
										beanName, normalizeHost(localHost));
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
					logger.info("Skipped export service [{}] for bean [{}#{}]@{} because {}=false", clazz.getName(),
							beanClassName, beanName, normalizeHost(localHost), key);
				} else {
					exportedServices.put(clazz.getName(), ctx.getBean(beanName));
					String description = remoting.description();
					if (StringUtils.isNotBlank(description))
						description = I18N.getText(description);
					exportedServiceDescriptions.put(clazz.getName(), description);
					logger.info("Exported service [{}] for bean [{}#{}]@{}", clazz.getName(), beanClassName, beanName,
							normalizeHost(localHost));
				}
			}
			for (Class<?> c : clazz.getInterfaces())
				export(c, beanName, beanClassName);
		}
	}

	@Override
	public void evict(String host) {
		for (Map.Entry<String, List<String>> entry : importedServiceCandidates.entrySet()) {
			List<String> hosts = entry.getValue();
			List<String> tobeRemoved = new ArrayList<>();
			for (String s : hosts)
				if (normalizeHost(s).equals(host) || s.indexOf(host) > 0)
					tobeRemoved.add(s);
			for (String s : tobeRemoved)
				hosts.remove(s);
		}
	}

	@Override
	public String discover(String serviceName, boolean polling) {
		String host = null;
		List<String> candidates = importedServiceCandidates.get(serviceName);
		if (candidates == null || candidates.size() == 0) {
			lookup(serviceName);
			candidates = importedServiceCandidates.get(serviceName);
		}
		candidates = distanceMeasurer.findNearest(getLocalHost(), candidates);
		boolean loadBalancing = !polling && (candidates != null && candidates.size() <= lbNodesThreshold);
		if (loadBalancing) {
			try {
				int consumers = 0;
				for (Map.Entry<String, Collection<String>> entry : getExportedHostsForService(serviceName).entrySet()) {
					if (candidates.contains(entry.getKey()) && (host == null || entry.getValue().size() < consumers)) {
						host = entry.getKey();
						consumers = entry.getValue().size();
					}
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
		if (host == null) {
			if (candidates != null && candidates.size() > 0)
				host = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
		}
		if (host != null) {
			onDiscover(serviceName, host);
			return normalizeHost(host);
		} else {
			throw new ServiceNotFoundException(serviceName);
		}
	}

	protected void onDiscover(String serviceName, String host) {
		logger.info("Discovered " + serviceName + " from " + host);
	}

	protected abstract void onReady();

	protected abstract void lookup(String serviceName);

	@PreDestroy
	public void destroy() {
		for (String serviceName : exportedServices.keySet())
			unregister(serviceName);
	}

	@EventListener
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if (event.getApplicationContext() == ctx)
			init();
	}

}
