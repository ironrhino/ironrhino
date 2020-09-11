package org.ironrhino.core.tracing;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.transaction.PlatformTransactionManager;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TracingTransactionManagerBeanFactoryPostProcessor implements BeanDefinitionRegistryPostProcessor {

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory)
			throws BeansException {

	}

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry) throws BeansException {
		try {
			String beanName = "transactionManager";
			String actualBeanName = "actualTransactionManager";
			BeanDefinition oldBd = beanDefinitionRegistry.getBeanDefinition(beanName);
			beanDefinitionRegistry.removeBeanDefinition(beanName);
			beanDefinitionRegistry.registerBeanDefinition(actualBeanName, oldBd);
			RootBeanDefinition newBd = new RootBeanDefinition(TracingTransactionManager.class);
			newBd.setPrimary(true);
			newBd.setTargetType(PlatformTransactionManager.class);
			newBd.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_NO);
			newBd.getConstructorArgumentValues().addIndexedArgumentValue(0, new RuntimeBeanReference(actualBeanName));
			beanDefinitionRegistry.registerBeanDefinition(beanName, newBd);
			log.info("Wrapped PlatformTransactionManager {} with {}", oldBd.getBeanClassName(),
					newBd.getBeanClassName());
		} catch (NoSuchBeanDefinitionException e) {
			// ignore
		}
	}

}