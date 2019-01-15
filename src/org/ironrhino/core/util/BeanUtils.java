package org.ironrhino.core.util;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.metadata.NotInCopy;
import org.ironrhino.core.model.Treeable;
import org.ironrhino.core.spring.converter.CustomConversionService;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.core.convert.ConversionService;

import com.google.common.base.Supplier;

import lombok.experimental.UtilityClass;

@UtilityClass
public class BeanUtils {

	public static boolean hasProperty(Class<?> clazz, String name) {
		if (org.springframework.beans.BeanUtils.getPropertyDescriptor(clazz, name) != null)
			return true;
		return false;
	}

	public static void copyPropertiesIfNotNull(Object source, Object target, String... properties) {
		if (properties.length == 0)
			return;
		BeanWrapperImpl bws = new BeanWrapperImpl(source);
		bws.setConversionService(CustomConversionService.getSharedInstance());
		BeanWrapperImpl bwt = new BeanWrapperImpl(target);
		bwt.setConversionService(CustomConversionService.getSharedInstance());
		for (String propertyName : properties) {
			Object value = bws.getPropertyValue(propertyName);
			if (value != null)
				bwt.setPropertyValue(propertyName, value);
		}
	}

	public static void copyProperties(Map<String, ?> source, Object target) {
		BeanWrapperImpl bw = new BeanWrapperImpl(target);
		bw.setConversionService(CustomConversionService.getSharedInstance());
		for (Map.Entry<String, ?> entry : source.entrySet()) {
			if (bw.isWritableProperty(entry.getKey()))
				bw.setPropertyValue(entry.getKey(), entry.getValue());
		}
	}

	public static void copyProperties(Object source, Object target, boolean ignoreNullValue, boolean ignoreNotInCopy,
			String... ignoreProperties) {
		Set<String> ignores = new HashSet<>();
		if (ignoreNotInCopy)
			ignores.addAll(AnnotationUtils.getAnnotatedPropertyNames(source.getClass(), NotInCopy.class));
		ignores.addAll(Arrays.asList(ignoreProperties));
		normalizeCollectionFields(source);
		BeanWrapperImpl bws = new BeanWrapperImpl(source);
		bws.setConversionService(CustomConversionService.getSharedInstance());
		PropertyDescriptor[] sourcePds = bws.getPropertyDescriptors();
		BeanWrapperImpl bwt = new BeanWrapperImpl(target);
		bwt.setConversionService(CustomConversionService.getSharedInstance());
		PropertyDescriptor[] targetPds = bwt.getPropertyDescriptors();
		for (PropertyDescriptor sourcePd : sourcePds) {
			if (sourcePd.getReadMethod() == null)
				continue;
			String name = sourcePd.getName();
			if (ignores.contains(name))
				continue;
			PropertyDescriptor targetPd = null;
			for (PropertyDescriptor pd : targetPds) {
				if (pd.getName().equals(name)) {
					targetPd = pd;
					break;
				}
			}
			if (targetPd == null || targetPd.getWriteMethod() == null)
				continue;
			Object value = bws.getPropertyValue(name);
			if (!ignoreNullValue || value != null)
				bwt.setPropertyValue(name, value);
		}
	}

	public static void copyProperties(Object source, Object target, boolean ignoreNotInCopy,
			String... ignoreProperties) {
		copyProperties(source, target, false, ignoreNotInCopy, ignoreProperties);
	}

	public static void copyProperties(Object source, Object target, String... ignoreProperties) {
		copyProperties(source, target, false, true, ignoreProperties);
	}

	@SuppressWarnings("unchecked")
	public static <S extends Treeable<S>> S copyTree(S source, String... ignoreProperties) {
		return (S) copyTree(source, () -> {
			try {
				return source.getClass().getConstructor().newInstance();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}, null, ignoreProperties);
	}

	public static <S extends Treeable<S>, T extends Treeable<T>> T copyTree(S source, Supplier<T> supplier,
			String... ignoreProperties) {
		return copyTree(source, supplier, null, ignoreProperties);
	}

	@SuppressWarnings("unchecked")
	public static <S extends Treeable<S>, T extends Treeable<T>> T copyTree(S source, Supplier<T> supplier,
			Predicate<S> filter, String... ignoreProperties) {
		if (filter != null && !filter.test(source))
			throw new IllegalArgumentException("source object self must be accepted if you specify a filter");
		T ret = supplier.get();
		Set<String> ignores = new HashSet<>();
		ignores.addAll(Arrays.asList(ignoreProperties));
		ignores.add("parent");
		ignores.add("children");
		copyProperties(source, ret, ignores.toArray(new String[ignores.size()]));
		for (S child : source.getChildren()) {
			if (filter == null || filter.test(child))
				ret.addChild(copyTree(child, supplier, filter, ignoreProperties));
		}
		return ret;
	}

	public static Object convert(Class<?> beanClass, String propertyName, String value) {
		try {
			return convert(beanClass.getConstructor().newInstance(), propertyName, value);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static Object convert(Object bean, String propertyName, String value) {
		BeanWrapperImpl bw = new BeanWrapperImpl(bean);
		bw.setConversionService(CustomConversionService.getSharedInstance());
		ConversionService cs = ApplicationContextUtils.getBean(ConversionService.class);
		if (cs != null)
			bw.setConversionService(cs);
		bw.setPropertyValue(propertyName, value);
		return bw.getPropertyValue(propertyName);
	}

	public static PropertyDescriptor getPropertyDescriptor(Class<?> beanClass, String propertyName) {
		if (propertyName.indexOf('.') == -1)
			return org.springframework.beans.BeanUtils.getPropertyDescriptor(beanClass, propertyName);
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

	public static void setPropertyValue(Object bean, String propertyName, Object propertyValue) {
		BeanWrapperImpl bw = new BeanWrapperImpl(bean);
		bw.setConversionService(CustomConversionService.getSharedInstance());
		createParentIfNull(bean, propertyName);
		bw.setPropertyValue(propertyName, propertyValue);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void normalizeCollectionFields(Object bean) {
		if (!(bean instanceof Serializable))
			return;
		try {
			for (Class<?> clazz = bean.getClass(); clazz != Object.class; clazz = clazz.getSuperclass()) {
				Field[] fields = clazz.getDeclaredFields();
				for (Field f : fields) {
					f.setAccessible(true);
					Object value = f.get(bean);
					if (value == null)
						continue;
					if (value.getClass().getName().startsWith("java."))
						continue;
					Class<?> declaredType = f.getType();
					if (declaredType == List.class) {
						List newValue = new ArrayList();
						newValue.addAll((List) value);
						f.set(bean, newValue);
					} else if (declaredType == Set.class) {
						Set newValue = new LinkedHashSet();
						newValue.addAll((Set) value);
						f.set(bean, newValue);
					} else if (declaredType == Map.class) {
						Map newValue = new LinkedHashMap();
						newValue.putAll((Map) value);
						f.set(bean, newValue);
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static boolean isEmpty(Object bean) {
		if (bean == null)
			return true;
		boolean b = true;
		BeanWrapperImpl bw = new BeanWrapperImpl(bean);
		for (PropertyDescriptor pd : bw.getPropertyDescriptors()) {
			if (pd.getReadMethod() != null && pd.getWriteMethod() != null) {
				if (bw.getPropertyValue(pd.getName()) != null) {
					b = false;
					break;
				}
			}
		}
		return b;
	}

	public static void createParentIfNull(Object bean, String nestedPath) {
		if (bean == null || StringUtils.isBlank(nestedPath))
			return;
		if (nestedPath.lastIndexOf('.') > nestedPath.lastIndexOf('['))
			nestedPath = nestedPath.substring(0, nestedPath.lastIndexOf('.'));
		else if (nestedPath.lastIndexOf('[') > nestedPath.lastIndexOf('.'))
			nestedPath = nestedPath.substring(0, nestedPath.lastIndexOf('['));
		else
			return;
		int end = 0;
		BeanWrapperImpl bw = new BeanWrapperImpl(bean);
		while (true) {
			end = nestedPath.indexOf('.', end + 1);
			String s = nestedPath.substring(0, end < 0 ? nestedPath.length() : end);
			if (s.indexOf('[') > 0) {
				s = s.substring(0, s.indexOf('['));
				end = -1;
			}
			PropertyDescriptor pd = bw.getPropertyDescriptor(s);
			if (pd == null || pd.getWriteMethod() == null)
				return;
			if (bw.getPropertyValue(s) != null) {
				if (end < 0)
					break;
				continue;
			}
			Class<?> type = pd.getPropertyType();
			Object value;
			if (Map.class.isAssignableFrom(type))
				value = new HashMap<>();
			else if (Set.class.isAssignableFrom(type))
				value = new LinkedHashMap<>();
			else if (Collection.class.isAssignableFrom(type))
				value = new ArrayList<>();
			else
				value = org.springframework.beans.BeanUtils.instantiateClass(type);
			bw.setPropertyValue(s, value);
			if (end < 0)
				break;
		}

	}

	public static <T> Function<Object, T> forCopy(Class<T> targetClass) {
		Function<Object, T> func = t -> {
			if (t == null)
				return null;
			try {
				T target = targetClass.getConstructor().newInstance();
				copyProperties(t, target, false);
				return target;
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				throw new RuntimeException(e);
			}
		};
		return func;
	}

	public static <T> Function<List<?>, List<T>> forCopyList(Class<T> targetClass) {
		Function<List<?>, List<T>> func = t -> {
			if (t == null)
				return null;
			List<T> list = new ArrayList<>(t.size());
			t.forEach(e -> {
				try {
					T target = targetClass.getConstructor().newInstance();
					copyProperties(e, target, false);
					list.add(target);
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException | NoSuchMethodException | SecurityException ex) {
					throw new RuntimeException(ex);
				}
			});
			return list;
		};
		return func;
	}

}
