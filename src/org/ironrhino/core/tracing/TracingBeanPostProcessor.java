package org.ironrhino.core.tracing;

import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;

import io.opentracing.contrib.jdbc.TracingDataSource;
import io.opentracing.util.GlobalTracer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TracingBeanPostProcessor implements BeanPostProcessor {

	static final BeanPostProcessor INSTANCE = new TracingBeanPostProcessor();

	static final BeanPostProcessor EMPTY = new BeanPostProcessor() {
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

}
