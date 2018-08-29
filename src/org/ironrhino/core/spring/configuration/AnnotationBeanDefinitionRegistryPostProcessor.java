package org.ironrhino.core.spring.configuration;

import java.lang.annotation.Annotation;
import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.spring.NameGenerator;
import org.ironrhino.core.util.ClassScanner;
import org.ironrhino.core.util.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.Environment;

import lombok.Getter;
import lombok.Setter;

public abstract class AnnotationBeanDefinitionRegistryPostProcessor<A extends Annotation, FB extends FactoryBean<?>>
		implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {

	protected Logger log = LoggerFactory.getLogger(getClass());

	private final Class<A> annotationClass;

	private final Class<FB> factoryBeanClass;

	@Getter
	@Setter
	private String[] packagesToScan;

	protected Environment env;

	@SuppressWarnings("unchecked")
	public AnnotationBeanDefinitionRegistryPostProcessor() {
		annotationClass = (Class<A>) ReflectionUtils.getGenericClass(getClass(), 0);
		factoryBeanClass = (Class<FB>) ReflectionUtils.getGenericClass(getClass(), 1);
	}

	@Override
	public void setEnvironment(Environment env) {
		this.env = env;
	}

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		Collection<Class<?>> annotatedClasses = ClassScanner.scanAnnotated(
				packagesToScan != null ? packagesToScan : ClassScanner.getAppPackages(), annotationClass);
		for (Class<?> annotatedClass : annotatedClasses) {
			if (!annotatedClass.isInterface())
				continue;
			String key = annotatedClass.getName() + ".imported";
			if ("false".equals(env.getProperty(key))) {
				log.info("Skipped import interface [{}] because {}=false", annotatedClass.getName(), key);
				continue;
			}
			A annotation = AnnotatedElementUtils.getMergedAnnotation(annotatedClass, annotationClass);
			String beanName = getExplicitBeanName(annotation);
			if (StringUtils.isBlank(beanName))
				beanName = NameGenerator.buildDefaultBeanName(annotatedClass.getName());
			if (registry.containsBeanDefinition(beanName)) {
				BeanDefinition bd = registry.getBeanDefinition(beanName);
				String beanClassName = bd.getBeanClassName();
				if (beanClassName == null || beanClassName.equals(factoryBeanClass.getName()))
					continue;
				try {
					Class<?> beanClass = Class.forName(beanClassName);
					if (annotatedClass.isAssignableFrom(beanClass)) {
						log.info("Skipped import interface [{}] because bean[{}#{}] exists", annotatedClass.getName(),
								beanClassName, beanName);
						continue;
					}
					if (bd instanceof RootBeanDefinition && FactoryBean.class.isAssignableFrom(beanClass)) {
						Class<?> targetType = ((RootBeanDefinition) bd).getTargetType();
						if (annotatedClass.isAssignableFrom(targetType)) {
							beanClassName = targetType.getName();
							log.info("Skipped import interface [{}] because bean[{}#{}] exists",
									annotatedClass.getName(), beanClassName, beanName);
							continue;
						}
					}
				} catch (ClassNotFoundException e) {
					log.error(e.getMessage(), e);
				}
				beanName = annotatedClass.getName();
			}
			RootBeanDefinition beanDefinition = new RootBeanDefinition(factoryBeanClass);
			beanDefinition.setTargetType(annotatedClass);
			beanDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_NO);
			processBeanDefinition(annotation, annotatedClass, beanDefinition);
			registry.registerBeanDefinition(beanName, beanDefinition);
			log.info("Register bean [{}] for @{} [{}]", beanName, annotationClass.getSimpleName(),
					annotatedClass.getName());
		}
	}

	protected abstract void processBeanDefinition(A annotation, Class<?> annotatedClass,
			RootBeanDefinition beanDefinition);

	protected abstract String getExplicitBeanName(A annotation);

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

	}
}