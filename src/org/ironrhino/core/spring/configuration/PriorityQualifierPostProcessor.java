package org.ironrhino.core.spring.configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class PriorityQualifierPostProcessor implements BeanPostProcessor, PriorityOrdered, BeanFactoryAware {

	private BeanFactory beanFactory;

	private Map<String, Boolean> logged = new ConcurrentHashMap<>();

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE + 1;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		ReflectionUtils.doWithFields(bean.getClass(), field -> {
			ReflectionUtils.makeAccessible(field);
			PriorityQualifier pq = field.getAnnotation(PriorityQualifier.class);
			String[] candidates = pq.value();
			if (candidates.length == 0)
				candidates = new String[] { field.getName() };
			for (String name : candidates) {
				if (beanFactory.containsBean(name)) {
					ResolvableType rt = ResolvableType.forField(field);
					boolean typeMatched = beanFactory.isTypeMatch(name, rt);
					if (!typeMatched) {
						typeMatched = beanFactory.isTypeMatch(name, rt.getRawClass());
					}
					if (typeMatched) {
						field.set(bean, beanFactory.getBean(name));
						if (logged.putIfAbsent(beanName + "." + field.getName(), true) == null) {
							// remove duplicated log for prototype bean
							log.info("Injected @PrioritizedQualifier(\"{}\") for field[{}] of bean[{}]", name,
									field.getName(), beanName);
						}
						break;
					} else {
						log.warn("Ignored @PrioritizedQualifier(\"{}\") for {} because it is not type of {}, ", name,
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