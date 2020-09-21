package org.ironrhino.core.tracing;

import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.transaction.PlatformTransactionManager;

import io.opentracing.contrib.jdbc.TracingDataSource;
import io.opentracing.util.GlobalTracer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TracingPostProcessor implements BeanPostProcessor, BeanDefinitionRegistryPostProcessor {

	static final TracingPostProcessor INSTANCE = new TracingPostProcessor();

	static final TracingPostProcessor EMPTY = new TracingPostProcessor() {
		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
			return bean;
		}

		@Override
		public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry bdr) throws BeansException {
		}
	};

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof DataSource) {
			bean = new TracingDataSource(GlobalTracer.get(), (DataSource) bean, null, true, null);
			log.info("Wrapped DataSource [{}] with {}", beanName, bean.getClass().getName());
		} else if (bean instanceof PlatformTransactionManager) {
			bean = new TracingTransactionManager((PlatformTransactionManager) bean);
			log.info("Wrapped PlatformTransactionManager [{}] with {}", beanName, bean.getClass().getName());
		}
		return bean;
	}

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry bdr) throws BeansException {
		BeanDefinition bd = new RootBeanDefinition();
		String className = TracingAspect.class.getName();
		bd.setBeanClassName(className);
		bdr.registerBeanDefinition(className, bd);
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory clfb) throws BeansException {
	}

}
