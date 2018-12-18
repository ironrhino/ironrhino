package org.ironrhino.core.remoting.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.ServletContext;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.remoting.DistanceMeasurer;
import org.ironrhino.core.remoting.Remoting;
import org.ironrhino.core.remoting.ServiceNotFoundException;
import org.ironrhino.core.remoting.ServiceRegistry;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.ReflectionUtils;
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

	private static final String PATH_DELIMITER = "///";

	private static final boolean IS_SERVER_PRESENT = ClassUtils.isPresent(CLASS_NAME_SERVER,
			AbstractServiceRegistry.class.getClassLoader());

	protected Logger logger = LoggerFactory.getLogger(getClass());

	@Value("${serviceRegistry.skipExport:false}")
	private boolean skipExport;

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

	private Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();

	@PostConstruct
	private void afterPropertiesSet() {
		localHost = AppInfo.getAppName() + '@' + AppInfo.getHostAddress() + ':'
				+ (AppInfo.getHttpPort() > 0 ? AppInfo.getHttpPort() : DEFAULT_HTTP_PORT);
		if (ctx instanceof ConfigurableWebApplicationContext) {
			ServletContext servletContext = ((ConfigurableWebApplicationContext) ctx).getServletContext();
			String ctxPath = servletContext != null ? servletContext.getContextPath() : "";
			if (!ctxPath.isEmpty())
				localHost += ctxPath;
		}
	}

	protected void init() {
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

	private void tryExport(Class<?> clazz, String beanName, String beanClassName) {
		Set<Class<?>> serviceInterfaces;
		Remoting remoting = AnnotatedElementUtils.getMergedAnnotation(clazz, Remoting.class);
		if (remoting != null) {
			serviceInterfaces = new HashSet<>();
			serviceInterfaces.addAll(Arrays.asList(remoting.serviceInterfaces()));
			if (serviceInterfaces.isEmpty())
				serviceInterfaces = ReflectionUtils.getAllInterfaces(clazz);
			if (serviceInterfaces.isEmpty()) {
				logger.warn("@Remoting on concrete class [{}] must assign interfaces to export services",
						clazz.getName());
			}
		} else {
			serviceInterfaces = ReflectionUtils.getAllInterfaces(clazz).stream()
					.filter(c -> c.isAnnotationPresent(Remoting.class)).collect(Collectors.toSet());
		}
		for (Class<?> serviceInterface : serviceInterfaces) {
			if (!serviceInterface.isInterface()) {
				logger.warn("Class [{}] in @Remoting on class [{}] must be interface", serviceInterface.getName(),
						clazz.getName());
			} else if (!serviceInterface.isAssignableFrom(clazz)) {
				logger.warn("Class [{}] must implements interface [{}] in @Remoting", clazz.getName(),
						serviceInterface.getName());
			} else {
				String key = serviceInterface.getName() + ".exported";
				if ("false".equals(ctx.getEnvironment().getProperty(key))) {
					logger.info("Skipped export service [{}] for bean [{}#{}]@{} because {}=false",
							serviceInterface.getName(), beanClassName, beanName, normalizeHost(localHost), key);
				} else {
					register(serviceInterface.getName(), ctx.getBean(beanName));
					logger.info("Exported service [{}] for bean [{}#{}]@{}", serviceInterface.getName(), beanClassName,
							beanName, normalizeHost(localHost));
				}
			}
		}
	}

	@Override
	public void register(String serviceName, String path, Object serviceObject) {
		exportedServices.put(serviceName, serviceObject);
		String description = exportedServiceDescriptions.get(serviceName);
		if (StringUtils.isBlank(description))
			exportedServiceDescriptions.put(serviceName,
					ctx.getEnvironment().getProperty(serviceName + ".description", ""));
		doRegister(serviceName, concatPath(getLocalHost(), path));
	}

	protected abstract void doRegister(String serviceName, String host);

	@Override
	public void unregister(String serviceName, String path) {
		exportedServices.remove(serviceName);
		exportedServiceDescriptions.remove(serviceName);
		doUnregister(serviceName, concatPath(getLocalHost(), path));
	}

	protected abstract void doUnregister(String serviceName, String host);

	@Override
	public void evict(String host) {
		for (Map.Entry<String, List<String>> entry : importedServiceCandidates.entrySet()) {
			List<String> hosts = entry.getValue();
			List<String> tobeRemoved = hosts.stream().filter(s -> isSame(s, host)).collect(Collectors.toList());
			hosts.removeAll(tobeRemoved);
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
				for (Map.Entry<String, Collection<String>> entry : getExportedHostsByService(serviceName).entrySet()) {
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
			// polling
			List<String> list = candidates;
			AtomicInteger counter = counters.computeIfAbsent(serviceName,
					s -> new AtomicInteger(ThreadLocalRandom.current().nextInt(list.size())));
			int current, next, size = list.size();
			do {
				current = counter.get();
				next = (current + 1) % list.size();
			} while (!counter.compareAndSet(current, next));
			host = list.get((next - 1 + size) % size); // list.get(next);
		}
		onDiscover(serviceName, stripPath(host), polling);
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

	@Override
	public Map<String, Collection<String>> getExportedHostsByService(String serviceName) {
		Map<String, Collection<String>> result = new TreeMap<>();
		doGetExportedHostsByService(serviceName).entrySet().stream()
				.forEach(entry -> result.put(stripPath(entry.getKey()), entry.getValue()));
		return result;
	}

	protected abstract Map<String, Collection<String>> doGetExportedHostsByService(String serviceName);

	@PreDestroy
	public void destroy() {
		for (String serviceName : exportedServices.keySet())
			unregister(serviceName);
	}

	@EventListener
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if (IS_SERVER_PRESENT) {
			if (CLASS_NAME_SERVER.equals(event.getApplicationContext().getId()))
				init();
		} else {
			if (event.getApplicationContext() == ctx)
				init();
		}
	}

	private static String normalizeHost(String host) {
		host = StringUtils.replace(host, PATH_DELIMITER, "");
		int i = host.indexOf('@');
		return i < 0 ? host : host.substring(i + 1);
	}

	private static String concatPath(String host, String path) {
		return StringUtils.isNotBlank(path) ? host + PATH_DELIMITER + path : host;
	}

	private static String stripPath(String host) {
		int index = host.indexOf(PATH_DELIMITER);
		return index > 0 ? host.substring(0, index) : host;
	}

	private static boolean isSame(String candidate, String evict) {
		if (candidate.equals(evict)) {
			// candidate: without path, host: unnormalized (from InstanceShutdownEvent)
			return true;
		}
		if (normalizeHost(candidate).equals(evict)) {
			// candidate: without path, host: normalized (from client)
			return true;
		}
		if (stripPath(candidate).equals(evict)) {
			// candidate: with path, host: unnormalized (from InstanceShutdownEvent)
			return true;
		}
		if (candidate.contains('@' + evict + PATH_DELIMITER)) {
			// candidate: with path, host: normalized (from client)
			return true;
		}
		return false;
	}

}
