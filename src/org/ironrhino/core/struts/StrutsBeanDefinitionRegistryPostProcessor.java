package org.ironrhino.core.struts;

import java.lang.reflect.Modifier;
import java.util.Collection;

import org.ironrhino.core.struts.result.AutoConfigResult;
import org.ironrhino.core.util.ClassScanner;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.Action;

@Component
public class StrutsBeanDefinitionRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor {

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		Collection<Class<?>> actionClasses = ClassScanner.scanAssignable(ClassScanner.getAppPackages(), Action.class);
		for (Class<?> actionClass : actionClasses) {
			if (isQualified(actionClass)) {
				RootBeanDefinition beanDefinition = new RootBeanDefinition(actionClass);
				beanDefinition.setScope(BeanDefinition.SCOPE_PROTOTYPE);
				beanDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_NAME);
				registry.registerBeanDefinition(actionClass.getName(), beanDefinition);
			}
		}

		Collection<Class<?>> resultClasses = ClassScanner.scanAssignable(AutoConfigResult.class.getPackage().getName(),
				AutoConfigResult.class);
		for (Class<?> resultClass : resultClasses) {
			if (isQualified(resultClass)) {
				RootBeanDefinition beanDefinition = new RootBeanDefinition(resultClass);
				beanDefinition.setScope(BeanDefinition.SCOPE_PROTOTYPE);
				beanDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_NAME);
				registry.registerBeanDefinition(resultClass.getName(), beanDefinition);
			}
		}
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

	}

	private static boolean isQualified(Class<?> clazz) {
		int mod = clazz.getModifiers();
		return Modifier.isPublic(mod) && !Modifier.isInterface(mod) && !Modifier.isAbstract(mod);
	}

}