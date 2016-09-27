package org.ironrhino.core.util;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.service.BaseManager;
import org.ironrhino.core.service.EntityManager;
import org.ironrhino.core.servlet.AppInfoListener;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Primary;
import org.springframework.core.ResolvableType;
import org.springframework.web.context.support.WebApplicationContextUtils;

public class ApplicationContextUtils {

	public static ApplicationContext getApplicationContext() {
		return WebApplicationContextUtils.getWebApplicationContext(AppInfoListener.SERVLET_CONTEXT);
	}

	@SuppressWarnings("unchecked")
	public static <T> T getBean(String name) {
		try {
			return (T) getApplicationContext().getBean(name);
		} catch (Exception e) {
			return null;
		}
	}

	public static <T> T getBean(Class<T> t) {
		try {
			return getApplicationContext().getBean(t);
		} catch (Exception e) {
			return null;
		}
	}

	public static <T> Map<String, T> getBeansOfType(Class<T> t) {
		try {
			return getApplicationContext().getBeansOfType(t);
		} catch (Exception e) {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public static <T extends Persistable<?>> BaseManager<T> getEntityManager(Class<T> entityClass) {
		String[] beanNames = getApplicationContext()
				.getBeanNamesForType(ResolvableType.forClassWithGenerics(BaseManager.class, entityClass));
		if (beanNames.length == 1) {
			return (BaseManager<T>) getApplicationContext().getBean(beanNames[0]);
		} else if (beanNames.length > 1) {
			for (String beanName : beanNames) {
				Object bean = getApplicationContext().getBean(beanName);
				if (bean.getClass().isAnnotationPresent(Primary.class))
					return (BaseManager<T>) bean;
			}
			for (String beanName : beanNames) {
				if (beanName.equals(StringUtils.uncapitalize(entityClass.getSimpleName()) + "Manager"))
					return (BaseManager<T>) getApplicationContext().getBean(beanName);
			}
			return (BaseManager<T>) getApplicationContext().getBean(beanNames[0]);
		} else {
			EntityManager<T> entityManager = getApplicationContext().getBean("entityManager", EntityManager.class);
			entityManager.setEntityClass(entityClass);
			return entityManager;
		}
	}

}