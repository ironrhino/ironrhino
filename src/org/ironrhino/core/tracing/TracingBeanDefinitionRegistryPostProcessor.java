package org.ironrhino.core.tracing;

import java.lang.reflect.Method;
import java.util.Collections;

import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.Bean;
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
		for (String beanName : beanDefinitionRegistry.getBeanDefinitionNames()) {
			try {
				BeanDefinition oldBd = beanDefinitionRegistry.getBeanDefinition(beanName);
				Class<?> targetType = null;
				String beanClassName = oldBd.getBeanClassName();
				if (beanClassName != null) {
					targetType = Class.forName(beanClassName);
				} else {
					String factoryBeanName = oldBd.getFactoryBeanName();
					String factoryMethodName = oldBd.getFactoryMethodName();
					if (factoryBeanName != null && factoryMethodName != null) {
						BeanDefinition fbd = beanDefinitionRegistry.getBeanDefinition(factoryBeanName);
						String fbClassName = fbd.getBeanClassName();
						if (fbClassName != null) {
							for (Method m : Class.forName(fbClassName).getMethods()) {
								if (m.getName().equals(factoryMethodName) && m.isAnnotationPresent(Bean.class)) {
									targetType = m.getReturnType();
									break;
								}
							}
						}
					}
				}
				if (targetType != null) {
					if (DataSource.class.isAssignableFrom(targetType)) {
						transformDataSource(beanDefinitionRegistry, beanName, oldBd);
					} else if (PlatformTransactionManager.class.isAssignableFrom(targetType)) {
						transformPlatformTransactionManager(beanDefinitionRegistry, beanName, oldBd);
					}
				}
			} catch (Exception ignored) {
				ignored.printStackTrace();
			}
		}
	}

	private void transformDataSource(BeanDefinitionRegistry beanDefinitionRegistry, String beanName,
			BeanDefinition oldBd) {
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
		log.info("Wrapped DataSource [{}] with {}", beanName, newBd.getBeanClassName());
	}

	private void transformPlatformTransactionManager(BeanDefinitionRegistry beanDefinitionRegistry, String beanName,
			BeanDefinition oldBd) {
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
		log.info("Wrapped PlatformTransactionManager [{}] with {}", beanName, newBd.getBeanClassName());
	}
}
