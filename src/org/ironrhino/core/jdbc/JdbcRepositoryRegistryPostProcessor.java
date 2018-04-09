package org.ironrhino.core.jdbc;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.spring.NameGenerator;
import org.ironrhino.core.util.ClassScanner;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JdbcRepositoryRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor {

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		Collection<Class<?>> jdbcRepositoryClasses = ClassScanner.scanAnnotated(ClassScanner.getAppPackages(),
				JdbcRepository.class);
		for (Class<?> jdbcRepositoryClass : jdbcRepositoryClasses) {
			if (!jdbcRepositoryClass.isInterface())
				continue;
			JdbcRepository annotation = AnnotatedElementUtils.getMergedAnnotation(jdbcRepositoryClass,
					JdbcRepository.class);
			if (annotation == null)
				continue;
			String beanName = annotation.name();
			if (StringUtils.isBlank(beanName)) {
				beanName = NameGenerator.buildDefaultBeanName(jdbcRepositoryClass.getName());
				if (registry.containsBeanDefinition(beanName))
					beanName = jdbcRepositoryClass.getName();
			}
			String dataSourceBeanName = annotation.dataSource();
			if (StringUtils.isBlank(dataSourceBeanName))
				dataSourceBeanName = "dataSource";
			String jdbcTemplate = annotation.jdbcTemplate();
			RootBeanDefinition beanDefinition = new RootBeanDefinition(JdbcRepositoryFactoryBean.class);
			beanDefinition.setTargetType(jdbcRepositoryClass);
			beanDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_NO);
			ConstructorArgumentValues constructorArgumentValues = new ConstructorArgumentValues();
			constructorArgumentValues.addIndexedArgumentValue(0, jdbcRepositoryClass);
			constructorArgumentValues.addIndexedArgumentValue(1,
					new RuntimeBeanReference(StringUtils.isNotBlank(jdbcTemplate) ? jdbcTemplate : dataSourceBeanName));
			beanDefinition.setConstructorArgumentValues(constructorArgumentValues);
			registry.registerBeanDefinition(beanName, beanDefinition);
			log.info("Register bean [{}] for @JdbcRepository [{}]", beanName, jdbcRepositoryClass.getName());
		}
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

	}
}