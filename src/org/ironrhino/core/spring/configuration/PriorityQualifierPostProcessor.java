package org.ironrhino.core.spring.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

@Component
public class PriorityQualifierPostProcessor implements BeanPostProcessor, BeanFactoryAware {

	private Logger logger = LoggerFactory.getLogger(getClass());

	private BeanFactory beanFactory;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		ReflectionUtils.doWithFields(bean.getClass(), field -> {
			ReflectionUtils.makeAccessible(field);
			PriorityQualifier pq = field.getAnnotation(PriorityQualifier.class);
			for (String name : pq.value()) {
				if (beanFactory.containsBean(name)) {
					ResolvableType rt = ResolvableType.forField(field);
					boolean typeMatched = beanFactory.isTypeMatch(name, rt);
					if (!typeMatched) {
						typeMatched = beanFactory.isTypeMatch(name, rt.getRawClass());
					}
					if (typeMatched) {
						field.set(bean, beanFactory.getBean(name));
						logger.info("Injected @PrioritizedQualifier(\"{}\") for field[{}] of bean[{}]", name,
								field.getName(), beanName);
						break;
					} else {
						logger.warn("Ignored @PrioritizedQualifier(\"{}\") for {} because it is not type of {}, ", name,
								beanName, rt);
					}
				}
			}
		}, field -> {
			return field.isAnnotationPresent(Autowired.class) && field.isAnnotationPresent(PriorityQualifier.class);
		});
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

}