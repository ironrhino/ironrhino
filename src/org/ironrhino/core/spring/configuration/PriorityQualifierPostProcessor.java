package org.ironrhino.core.spring.configuration;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

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
import org.springframework.util.StringUtils;

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
			inject(bean, beanName, field);
		}, this::filter);

		ReflectionUtils.doWithMethods(bean.getClass(), method -> {
			inject(bean, beanName, method);
		}, this::filter);
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	private void inject(Object bean, String beanName, Method method) {
		ReflectionUtils.makeAccessible(method);
		doInject(bean, beanName, method, () -> {
			String methodName = method.getName();
			if (methodName.startsWith("set") && methodName.length() > 3)
				methodName = StringUtils.uncapitalize(methodName.substring(3));
			return methodName;
		}, () -> ResolvableType.forMethodParameter(method, 0), (b, candidate) -> {
			try {
				method.invoke(b, candidate);
			} catch (IllegalAccessException | InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		});
	}

	private void inject(Object bean, String beanName, Field field) {
		ReflectionUtils.makeAccessible(field);
		doInject(bean, beanName, field, field::getName, () -> ResolvableType.forField(field), (b, candidate) -> {
			try {
				field.set(b, candidate);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		});
	}

	private void doInject(Object bean, String beanName, AccessibleObject methodOrField,
			Supplier<String> defaultCandidate, Supplier<ResolvableType> typeSupplier,
			BiConsumer<Object, Object> injectConsumer) {
		String injectPoint = defaultCandidate.get();
		PriorityQualifier pq = methodOrField.getAnnotation(PriorityQualifier.class);
		String[] candidates = pq.value();
		if (candidates.length == 0)
			candidates = new String[] { injectPoint };
		for (String name : candidates) {
			if (beanFactory.containsBean(name)) {
				ResolvableType rt = typeSupplier.get();

				boolean typeMatched = beanFactory.isTypeMatch(name, rt);
				if (!typeMatched) {
					Class<?> rawClass = rt.getRawClass();
					typeMatched = (rawClass != null) && beanFactory.isTypeMatch(name, rawClass);
				}
				if (typeMatched) {
					injectConsumer.accept(bean, beanFactory.getBean(name));
					if (logged.putIfAbsent(beanName + "." + injectPoint, true) == null) {
						// remove duplicated log for prototype bean
						log.info("Injected @PrioritizedQualifier(\"{}\") for field[{}] of bean[{}]", name, injectPoint,
								beanName);
					}
					break;
				} else {
					log.warn("Ignored @PrioritizedQualifier(\"{}\") for {} because it is not type of {}, ", name,
							beanName, rt);
				}
			}
		}
	}

	private boolean filter(AccessibleObject methodOrField) {
		return methodOrField.isAnnotationPresent(Autowired.class)
				&& methodOrField.isAnnotationPresent(PriorityQualifier.class);
	}

}