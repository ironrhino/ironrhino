package org.ironrhino.core.remoting.client;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

import org.ironrhino.core.remoting.Remoting;
import org.ironrhino.core.spring.NameGenerator;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.ClassScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;

public abstract class RemotingServiceRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor {

	protected Logger logger = LoggerFactory.getLogger(getClass());

	protected abstract String[] getBasePackages();

	protected Collection<Class<?>> getIncludeClasses() {
		return Collections.emptySet();
	}

	protected Collection<Class<?>> getExcludeClasses() {
		return Collections.emptySet();
	}

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		Collection<Class<?>> remotingServices = new LinkedHashSet<Class<?>>();
		Collection<Class<?>> includeClasses = getIncludeClasses();
		if (includeClasses != null)
			remotingServices.addAll(includeClasses);
		remotingServices.addAll(ClassScanner.scanAnnotated(getBasePackages(), Remoting.class));
		Collection<Class<?>> excludeClasses = getExcludeClasses();
		for (Class<?> remotingService : remotingServices) {
			if (!remotingService.isInterface() || excludeClasses != null && excludeClasses.contains(remotingService))
				continue;
			String key = remotingService.getName() + ".imported";
			if ("false".equals(AppInfo.getApplicationContextProperties().getProperty(key))) {
				logger.info("skiped import service [{}] because {}=false", remotingService.getName(), key);
				continue;
			}
			String beanName = NameGenerator.buildDefaultBeanName(remotingService.getName());
			if (registry.containsBeanDefinition(beanName)) {
				BeanDefinition bd = registry.getBeanDefinition(beanName);
				String beanClassName = bd.getBeanClassName();
				if (beanClassName.startsWith("org.ironrhino.core.remoting.client.") && beanClassName.endsWith("Client")
						&& remotingService.getName().equals(bd.getPropertyValues().get("serviceInterface")))
					continue;
				try {
					Class<?> beanClass = Class.forName(beanClassName);
					if (remotingService.isAssignableFrom(beanClass))
						continue;
				} catch (ClassNotFoundException e) {
					logger.error(e.getMessage(), e);
					e.printStackTrace();
				}
				beanName = remotingService.getName();
			}
			RootBeanDefinition beanDefinition = new RootBeanDefinition(HttpInvokerClient.class);
			beanDefinition.setTargetType(remotingService);
			beanDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_NAME);
			MutablePropertyValues propertyValues = new MutablePropertyValues();
			propertyValues.addPropertyValue("serviceInterface", remotingService.getName());
			beanDefinition.setPropertyValues(propertyValues);
			registry.registerBeanDefinition(beanName, beanDefinition);
			logger.info("imported service [{}] for bean [{}#{}]", remotingService.getName(),
					beanDefinition.getBeanClassName(), beanName);
		}
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

	}
}