package org.ironrhino.core.tracing;

import java.util.Collections;

import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.transaction.PlatformTransactionManager;

import io.opentracing.contrib.jdbc.TracingDataSource;
import io.opentracing.util.GlobalTracer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TracingBeanDefinitionRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor {

	static final BeanDefinitionRegistryPostProcessor INSTANCE = new TracingBeanDefinitionRegistryPostProcessor();

	static final BeanDefinitionRegistryPostProcessor EMPTY = new BeanDefinitionRegistryPostProcessor() {

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory arg0) throws BeansException {

		}

		@Override
		public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry arg0) throws BeansException {

		}
	};

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory)
			throws BeansException {
	}

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry) throws BeansException {
		try {
			String beanName = "dataSource";
			BeanDefinition oldBd = beanDefinitionRegistry.getBeanDefinition(beanName);
			beanDefinitionRegistry.removeBeanDefinition(beanName);
			RootBeanDefinition newBd = new RootBeanDefinition(TracingDataSource.class);
			newBd.setTargetType(DataSource.class);
			newBd.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_NO);
			newBd.getConstructorArgumentValues().addIndexedArgumentValue(0,
					new ConstructorArgumentValues.ValueHolder(GlobalTracer.get()));
			newBd.getConstructorArgumentValues().addIndexedArgumentValue(1, oldBd);
			newBd.getConstructorArgumentValues().addIndexedArgumentValue(2,
					new ConstructorArgumentValues.ValueHolder(null));
			newBd.getConstructorArgumentValues().addIndexedArgumentValue(3,
					new ConstructorArgumentValues.ValueHolder(true));
			newBd.getConstructorArgumentValues().addIndexedArgumentValue(4,
					new ConstructorArgumentValues.ValueHolder(Collections.emptySet()));
			if (oldBd.isPrimary()) {
				newBd.setPrimary(true);
				oldBd.setPrimary(false);
			}
			beanDefinitionRegistry.registerBeanDefinition(beanName, newBd);
			log.info("Wrapped DataSource with {}", newBd.getBeanClassName());
		} catch (NoSuchBeanDefinitionException ignored) {
		}
		try {
			String beanName = "transactionManager";
			BeanDefinition oldBd = beanDefinitionRegistry.getBeanDefinition(beanName);
			beanDefinitionRegistry.removeBeanDefinition(beanName);
			RootBeanDefinition newBd = new RootBeanDefinition(TracingTransactionManager.class);
			newBd.setTargetType(PlatformTransactionManager.class);
			newBd.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_NO);
			newBd.getConstructorArgumentValues().addIndexedArgumentValue(0, oldBd);
			if (oldBd.isPrimary()) {
				newBd.setPrimary(true);
				oldBd.setPrimary(false);
			}
			beanDefinitionRegistry.registerBeanDefinition(beanName, newBd);
			log.info("Wrapped PlatformTransactionManager with {}", newBd.getBeanClassName());
		} catch (NoSuchBeanDefinitionException ignored) {
		}
	}

}
