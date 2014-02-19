package org.ironrhino.core.util;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.service.BaseManager;
import org.ironrhino.core.service.EntityManager;
import org.ironrhino.core.servlet.AppInfoListener;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

public class ApplicationContextUtils {

	public static ApplicationContext getApplicationContext() {
		return WebApplicationContextUtils
				.getWebApplicationContext(AppInfoListener.SERVLET_CONTEXT);
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
	public static <T extends Persistable<?>> BaseManager<T> getEntityManager(
			Class<T> entityClass) {
		String entityManagerName = StringUtils.uncapitalize(entityClass
				.getSimpleName()) + "Manager";
		try {
			return (BaseManager<T>) getApplicationContext().getBean(
					entityManagerName);
		} catch (NoSuchBeanDefinitionException e) {
			EntityManager<T> entityManager = ApplicationContextUtils
					.getBean(EntityManager.class);
			entityManager.setEntityClass(entityClass);
			return entityManager;
		}
	}

}