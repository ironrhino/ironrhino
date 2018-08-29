package org.ironrhino.core.remoting.client;

import java.util.Collection;

import org.ironrhino.core.remoting.Remoting;
import org.ironrhino.core.spring.NameGenerator;
import org.ironrhino.core.util.ClassScanner;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RemotingServiceRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {

	@Getter
	@Setter
	private String[] packagesToScan;

	protected Environment env;

	@Override
	public void setEnvironment(Environment env) {
		this.env = env;
	}

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		Collection<Class<?>> remotingServices = ClassScanner
				.scanAnnotated(packagesToScan != null ? packagesToScan : ClassScanner.getAppPackages(), Remoting.class);
		for (Class<?> remotingService : remotingServices) {
			if (!remotingService.isInterface())
				continue;
			String key = remotingService.getName() + ".imported";
			if ("false".equals(env.getProperty(key))) {
				log.info("Skipped import service [{}] because {}=false", remotingService.getName(), key);
				continue;
			}
			String beanName = NameGenerator.buildDefaultBeanName(remotingService.getName());
			if (registry.containsBeanDefinition(beanName)) {
				BeanDefinition bd = registry.getBeanDefinition(beanName);
				String beanClassName = bd.getBeanClassName();
				if (beanClassName == null)
					continue;
				if (beanClassName.startsWith("org.ironrhino.core.remoting.client.") && beanClassName.endsWith("Client")
						&& remotingService.getName().equals(bd.getPropertyValues().get("serviceInterface")))
					continue;
				try {
					Class<?> beanClass = Class.forName(beanClassName);
					if (remotingService.isAssignableFrom(beanClass)) {
						log.info("Skipped import service [{}] because bean[{}#{}] exists", remotingService.getName(),
								beanClassName, beanName);
						continue;
					}
					if (bd instanceof RootBeanDefinition && FactoryBean.class.isAssignableFrom(beanClass)) {
						Class<?> targetType = ((RootBeanDefinition) bd).getTargetType();
						if (remotingService.isAssignableFrom(targetType)) {
							beanClassName = targetType.getName();
							log.info("Skipped import service [{}] because bean[{}#{}] exists",
									remotingService.getName(), beanClassName, beanName);
							continue;
						}
					}
				} catch (ClassNotFoundException e) {
					log.error(e.getMessage(), e);
					e.printStackTrace();
				}
				beanName = remotingService.getName();
			}
			RootBeanDefinition beanDefinition = new RootBeanDefinition(HttpInvokerClient.class);
			beanDefinition.setTargetType(remotingService);
			beanDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_NO);
			MutablePropertyValues propertyValues = new MutablePropertyValues();
			propertyValues.addPropertyValue("serviceInterface", remotingService.getName());
			beanDefinition.setPropertyValues(propertyValues);
			registry.registerBeanDefinition(beanName, beanDefinition);
			log.info("Imported service [{}] for bean [{}#{}]", remotingService.getName(),
					beanDefinition.getBeanClassName(), beanName);
		}
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

	}
}