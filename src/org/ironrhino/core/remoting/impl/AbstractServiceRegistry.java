package org.ironrhino.core.remoting.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.ConfigurableWebApplicationContext;

import lombok.Getter;

public abstract class AbstractServiceRegistry implements ServiceRegistry {

	private static final String CLASS_NAME_SERVER = "org.ironrhino.core.remoting.server.HttpInvokerServer";
	private static final String CLASS_NAME_CLIENT = "org.ironrhino.core.remoting.client.HttpInvokerClient";
	private static final boolean IS_SERVER_PRESENT = ClassUtils.isPresent(CLASS_NAME_SERVER,
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
	private Map<String, List<String>> importedServiceCandidates = new ConcurrentHashMap<>();

	@Getter
	private Map<String, Object> exportedServices = new ConcurrentHashMap<>();

	protected Map<String, String> exportedServiceDescriptions = new ConcurrentHashMap<>();

	protected Map<String, String> importedServices = new ConcurrentHashMap<>();

	protected boolean ready;

	@Getter
	private String localHost;

	protected void init() {
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
				importedServiceCandidates.put(serviceName, new CopyOnWriteArrayList<String>());
			} else {
				if (clazz != null && FactoryBean.class.isAssignableFrom(clazz) && bd instanceof RootBeanDefinition) {
					clazz = ((RootBeanDefinition) bd).getTargetType();
					if (clazz == null)
						continue;
					beanClassName = clazz.getName();
				}
				if (IS_SERVER_PRESENT && !skipExport)
					tryExport(clazz, beanName, beanClassName);
			}
		}
		for (String serviceName : importedServiceCandidates.keySet())
			lookup(serviceName);
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

	private void tryExport(Class<?> clazz, String beanName, String beanClassName) {
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
							doExport(remoting, inte, beanName, beanClassName);
						}
					}
				}
			}
			for (Class<?> inte : clazz.getInterfaces()) {
				tryExport(inte, beanName, beanClassName);
			}
		} else {
			Remoting remoting = clazz.getAnnotation(Remoting.class);
			if (remoting != null) {
				doExport(remoting, clazz, beanName, beanClassName);
			}
			for (Class<?> inte : clazz.getInterfaces())
				tryExport(inte, beanName, beanClassName);
		}
	}

	private void doExport(Remoting remoting, Class<?> serviceInterface, String beanName, String beanClassName) {
		String key = serviceInterface.getName() + ".exported";
		if ("false".equals(ctx.getEnvironment().getProperty(key))) {
			logger.info("Skipped export service [{}] for bean [{}#{}]@{} because {}=false", serviceInterface.getName(),
					beanClassName, beanName, normalizeHost(localHost), key);
		} else {
			register(serviceInterface.getName(), ctx.getBean(beanName));
			String description = remoting.description();
			if (StringUtils.isNotBlank(description))
				exportedServiceDescriptions.put(serviceInterface.getName(), description);
			logger.info("Exported service [{}] for bean [{}#{}]@{}", serviceInterface.getName(), beanClassName,
					beanName, normalizeHost(localHost));
		}
	}

	@Override
	public void register(String serviceName, Object serviceObject) {
		exportedServices.put(serviceName, serviceObject);
		String description = exportedServiceDescriptions.get(serviceName);
		if (StringUtils.isBlank(description))
			exportedServiceDescriptions.put(serviceName,
					ctx.getEnvironment().getProperty(serviceName + ".description", ""));
		doRegister(serviceName, getLocalHost());
	}

	protected abstract void doRegister(String serviceName, String host);

	@Override
	public void unregister(String serviceName) {
		exportedServices.remove(serviceName);
		exportedServiceDescriptions.remove(serviceName);
		doUnregister(serviceName, getLocalHost());
	}

	protected abstract void doUnregister(String serviceName, String host);

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
		List<String> candidates = importedServiceCandidates.get(serviceName);
		if (CollectionUtils.isEmpty(candidates)) {
			lookup(serviceName);
			candidates = importedServiceCandidates.get(serviceName);
		}
		if (CollectionUtils.isEmpty(candidates))
			throw new ServiceNotFoundException(serviceName);
		String host = null;
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
		if (host == null)
			host = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
		onDiscover(serviceName, host, polling);
		return normalizeHost(host);
	}

	protected void onDiscover(String serviceName, String host, boolean polling) {
		importedServices.put(serviceName, host);
		if (!polling) {
			logger.info("Discovered " + serviceName + " from " + host);
			if (ready)
				writeDiscoveredServices();
		}
	}

	protected void onReady() {
		writeDiscoveredServices();
		writeExportServiceDescriptions();
		ready = true;
	}

	protected abstract void writeDiscoveredServices();

	protected abstract void writeExportServiceDescriptions();

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
