package org.ironrhino.core.remoting.client;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

import org.ironrhino.core.remoting.Remoting;
import org.ironrhino.core.util.ClassScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.util.StringUtils;

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
		String[] basePackages = getBasePackages();
		if (basePackages != null)
			remotingServices.addAll(ClassScanner.scanAnnotated(basePackages, Remoting.class));
		Collection<Class<?>> excludeClasses = getExcludeClasses();
		for (Class<?> remotingService : remotingServices) {
			if (!remotingService.isInterface() || excludeClasses != null && excludeClasses.contains(remotingService))
				continue;
			String beanName = StringUtils.uncapitalize(remotingService.getSimpleName());
			if (registry.containsBeanDefinition(beanName))
				beanName = remotingService.getName();
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