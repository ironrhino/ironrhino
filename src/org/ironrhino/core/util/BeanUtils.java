package org.ironrhino.core.util;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ironrhino.core.metadata.NotInCopy;
import org.ironrhino.core.model.BaseTreeableEntity;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.core.convert.ConversionService;

public class BeanUtils {

	public static boolean hasProperty(Class<?> clazz, String name) {
		if (org.springframework.beans.BeanUtils.getPropertyDescriptor(clazz,
				name) != null)
			return true;
		return false;
	}

	public static void copyPropertiesIfNotNull(Object source, Object target,
			String... properties) {
		if (properties.length == 0)
			return;
		BeanWrapperImpl bws = new BeanWrapperImpl(source);
		BeanWrapperImpl bwt = new BeanWrapperImpl(target);
		for (String propertyName : properties) {
			Object value = bws.getPropertyValue(propertyName);
			if (value != null)
				bwt.setPropertyValue(propertyName, value);
		}
	}

	public static void copyProperties(Map<String, Object> source,
			Object target, String... ignoreProperties) {
		BeanWrapperImpl bw = new BeanWrapperImpl(target);
		for (Map.Entry<String, Object> entry : source.entrySet()) {
			if (bw.isWritableProperty(entry.getKey()))
				bw.setPropertyValue(entry.getKey(), entry.getValue());
		}
	}

	public static void copyProperties(Object source, Object target,
			String... ignoreProperties) {
		Set<String> ignores = new HashSet<String>();
		ignores.addAll(AnnotationUtils.getAnnotatedPropertyNames(
				source.getClass(), NotInCopy.class));
		ignores.addAll(Arrays.asList(ignoreProperties));
		org.springframework.beans.BeanUtils.copyProperties(source, target,
				ignores.toArray(ignoreProperties));
	}

	public static <T extends BaseTreeableEntity<T>> T deepClone(T source,
			String... ignoreProperties) {
		return deepClone(source, null, ignoreProperties);
	}

	@SuppressWarnings("unchecked")
	public static <T extends BaseTreeableEntity<T>> T deepClone(T source,
			ObjectFilter filter, String... ignoreProperties) {
		if (filter != null && !filter.accept(source))
			throw new IllegalArgumentException(
					"source object self must be accepted if you specify a filter");
		T ret = null;
		try {
			ret = (T) source.getClass().newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		copyProperties(source, ret, ignoreProperties);
		List<T> children = new ArrayList<T>();
		for (T child : source.getChildren()) {
			if (filter == null || filter.accept(child)) {
				T t = deepClone(child, filter, ignoreProperties);
				t.setParent(ret);
				children.add(t);
			}
		}
		ret.setChildren(children);
		return ret;

	}

	public static Object convert(Class<?> beanClass, String propertyName,
			String value) {
		try {
			return convert(beanClass.newInstance(), propertyName, value);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static Object convert(Object bean, String propertyName, String value) {
		BeanWrapperImpl bw = new BeanWrapperImpl(bean);
		ConversionService cs = ApplicationContextUtils
				.getBean(ConversionService.class);
		if (cs != null)
			bw.setConversionService(cs);
		bw.setPropertyValue(propertyName, value);
		return bw.getPropertyValue(propertyName);
	}

	public static PropertyDescriptor getPropertyDescriptor(Class<?> beanClass,
			String propertyName) {
		if (propertyName.indexOf('.') == -1)
			return org.springframework.beans.BeanUtils.getPropertyDescriptor(
					beanClass, propertyName);
		String[] arr = propertyName.split("\\.");
		PropertyDescriptor pd = null;
		int i = 0;
		Class<?> clazz = beanClass;
		while (i < arr.length) {
			pd = BeanUtils.getPropertyDescriptor(clazz, arr[i]);
			if (pd == null)
				return null;
			clazz = pd.getPropertyType();
			i++;
		}
		return pd;
	}

	public static void setPropertyValue(Object bean, String propertyName,
			Object propertyValue) {
		BeanWrapperImpl bw = new BeanWrapperImpl(bean);
		if (propertyName.indexOf('.') == -1) {
			bw.setPropertyValue(propertyName, propertyValue);
			return;
		}
		String[] arr = propertyName.split("\\.");
		int i = 0;
		String name = null;
		while (i < arr.length - 1) {
			if (name == null)
				name = arr[i];
			else
				name += "." + arr[i];
			Object value = bw.getPropertyValue(name);
			if (value == null) {
				try {
					value = getPropertyDescriptor(bean.getClass(), name)
							.getPropertyType().newInstance();
					bw.setPropertyValue(name, value);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
			i++;
		}
		bw.setPropertyValue(propertyName, propertyValue);
	}
}
