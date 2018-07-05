package org.ironrhino.core.util;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.service.BaseManager;
import org.ironrhino.core.service.EntityManager;
import org.ironrhino.core.servlet.MainAppInitializer;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Primary;
import org.springframework.core.ResolvableType;
import org.springframework.web.context.support.WebApplicationContextUtils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ApplicationContextUtils {

	public static ApplicationContext getApplicationContext() {
		return WebApplicationContextUtils.getWebApplicationContext(MainAppInitializer.SERVLET_CONTEXT);
	}

	@SuppressWarnings("unchecked")
	public static <T> T getBean(String name) {
		try {
			return (T) getApplicationContext().getBean(name);
		} catch (NoSuchBeanDefinitionException e) {
			if (name.indexOf('.') > 0) {
				try {
					Class<T> clazz = (Class<T>) Class.forName(name);
					return getBean(clazz);
				} catch (ClassNotFoundException e1) {
					return null;
				}
			} else {
				return null;
			}
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
	public static <PK extends Serializable, T extends Persistable<PK>> BaseManager<PK, T> getEntityManager(
			Class<T> entityClass) {
		String[] beanNames;
		try {
			beanNames = getApplicationContext().getBeanNamesForType(ResolvableType.forClassWithGenerics(
					BaseManager.class, entityClass.getMethod("getId").getReturnType(), entityClass));
			if (beanNames.length == 1) {
				return (BaseManager<PK, T>) getApplicationContext().getBean(beanNames[0]);
			}
		} catch (NoSuchMethodException e) {
			beanNames = new String[0];
		}
		if (beanNames.length > 1) {
			for (String beanName : beanNames) {
				Object bean = getApplicationContext().getBean(beanName);
				if (bean.getClass().isAnnotationPresent(Primary.class))
					return (BaseManager<PK, T>) bean;
			}
			for (String beanName : beanNames) {
				if (beanName.equals(StringUtils.uncapitalize(entityClass.getSimpleName()) + "Manager"))
					return (BaseManager<PK, T>) getApplicationContext().getBean(beanName);
			}
			return (BaseManager<PK, T>) getApplicationContext().getBean(beanNames[0]);
		} else {
			EntityManager<PK, T> entityManager = getApplicationContext().getBean("entityManager", EntityManager.class);
			entityManager.setEntityClass(entityClass);
			return entityManager;
		}
	}

}