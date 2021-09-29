package org.ironrhino.core.tracing;

import java.lang.reflect.Method;

import javax.sql.DataSource;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
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
			ProxyFactory pf = new ProxyFactory(bean);
			pf.addAdvice(new MethodInterceptor() {
				@Override
				public Object invoke(MethodInvocation invocation) throws Throwable {
					Method m = invocation.getMethod();
					if (m.getDeclaringClass() == PlatformTransactionManager.class) {
						return Tracing.executeCheckedCallable("transactionManager." + m.getName(),
								() -> invocation.proceed(), "component", "tx");
					}
					return invocation.proceed();
				}
			});
			bean = pf.getProxy();
			log.info("Proxied PlatformTransactionManager [{}] with tracing supports", beanName);
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
