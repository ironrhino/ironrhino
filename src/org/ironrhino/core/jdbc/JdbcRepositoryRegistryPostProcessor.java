package org.ironrhino.core.jdbc;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.spring.configuration.AnnotationBeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.stereotype.Component;

@Component
public class JdbcRepositoryRegistryPostProcessor
		extends AnnotationBeanDefinitionRegistryPostProcessor<JdbcRepository, JdbcRepositoryFactoryBean> {

	@Override
	public void processBeanDefinition(JdbcRepository annotation, Class<?> annotatedClass,
			RootBeanDefinition beanDefinition) {
		String dataSourceBeanName = annotation.dataSource();
		if (StringUtils.isBlank(dataSourceBeanName))
			dataSourceBeanName = "dataSource";
		String jdbcTemplate = annotation.jdbcTemplate();
		ConstructorArgumentValues constructorArgumentValues = new ConstructorArgumentValues();
		constructorArgumentValues.addIndexedArgumentValue(0, annotatedClass);
		constructorArgumentValues.addIndexedArgumentValue(1,
				new RuntimeBeanReference(StringUtils.isNotBlank(jdbcTemplate) ? jdbcTemplate : dataSourceBeanName));
		beanDefinition.setConstructorArgumentValues(constructorArgumentValues);
	}

	@Override
	protected String getExplicitBeanName(JdbcRepository annotation) {
		return annotation.name();
	}

}