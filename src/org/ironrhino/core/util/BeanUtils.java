package org.ironrhino.core.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ironrhino.core.metadata.NotInCopy;
import org.ironrhino.core.model.BaseTreeableEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.core.convert.ConversionService;

public class BeanUtils {

	private static final Logger log = LoggerFactory.getLogger(BeanUtils.class);

	public static boolean hasProperty(Class<?> clazz, String name) {
		if (org.springframework.beans.BeanUtils.getPropertyDescriptor(clazz,
				name) != null)
			return true;
		return false;
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
		try {
			T ret = (T) source.getClass().newInstance();
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
		} catch (Exception e) {
			log.error("exception occurs", e);
			return null;
		}
	}

	public static Object convert(Class<?> beanClass, String propertyName,
			String value) throws Exception {
		return convert(beanClass.newInstance(), propertyName, value);
	}

	public static Object convert(Object bean, String propertyName, String value) {
		BeanWrapperImpl bw = new BeanWrapperImpl(bean);
		bw.setConversionService(ApplicationContextUtils
				.getBean(ConversionService.class));
		bw.setPropertyValue(propertyName, value);
		return bw.getPropertyValue(propertyName);
	}

}
