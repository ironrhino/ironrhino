package org.ironrhino.rest;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.ironrhino.core.remoting.ServiceRegistry;
import org.ironrhino.core.util.ReflectionUtils;
import org.ironrhino.rest.client.RestApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.bind.annotation.RestController;

public class ApiRegistrant {

	@Autowired
	private ConfigurableApplicationContext ctx;

	@Autowired(required = false)
	private ServiceRegistry serviceRegistry;

	private Set<Class<?>> serviceInterfaces = new HashSet<>();

	private final String servletPath;

	public ApiRegistrant(String servletMapping) {
		if (servletMapping.endsWith("/*"))
			servletMapping = servletMapping.substring(0, servletMapping.length() - 2);
		servletPath = servletMapping;
	}

	@PostConstruct
	private void register() {
		if (serviceRegistry == null)
			return;
		for (String name : ctx.getBeanNamesForAnnotation(RestController.class)) {
			BeanDefinition bd = ctx.getBeanFactory().getBeanDefinition(name);
			String beanClassName = bd.getBeanClassName();
			if (beanClassName == null)
				continue;
			Class<?> clazz = null;
			try {
				clazz = Class.forName(beanClassName);
			} catch (Exception e) {
				continue;
			}
			serviceInterfaces.addAll(ReflectionUtils.getAllInterfaces(clazz).stream()
					.filter(c -> c.isAnnotationPresent(RestApi.class)).collect(Collectors.toList()));
		}
		for (Class<?> clz : serviceInterfaces)
			serviceRegistry.register(clz.getName(), servletPath, ctx.getBean(clz));
	}

	@PreDestroy
	private void unregister() {
		if (serviceRegistry == null)
			return;
		for (Class<?> clz : serviceInterfaces)
			serviceRegistry.unregister(clz.getName(), servletPath);
	}

}