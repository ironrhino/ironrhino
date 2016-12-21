package org.ironrhino.core.jdbc;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.JsonUtils;
import org.ironrhino.core.util.ReflectionUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;

public class EntityBeanPropertySqlParameterSource extends BeanPropertySqlParameterSource {

	private final BeanWrapper beanWrapper;

	public EntityBeanPropertySqlParameterSource(Object object) {
		super(object);
		beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(object);
	}

	@Override
	public boolean hasValue(String name) {
		boolean b = super.hasValue(name);
		if (b)
			return b;
		for (PropertyDescriptor pd : beanWrapper.getPropertyDescriptors()) {
			Column column = pd.getReadMethod().getAnnotation(Column.class);
			if (column == null) {
				try {
					column = ReflectionUtils.getField(beanWrapper.getWrappedClass(), pd.getName())
							.getAnnotation(Column.class);
				} catch (NoSuchFieldException e) {
				}
			}
			if (column != null && name.equals(column.name()))
				return true;
		}
		return false;
	}

	@Override
	public Object getValue(String name) throws IllegalArgumentException {
		if (beanWrapper.isReadableProperty(name)) {
			Object value = beanWrapper.getPropertyValue(name);
			if (value instanceof Enum) {
				Method getter = beanWrapper.getPropertyDescriptor(name).getReadMethod();
				Enumerated enumerated = getter.getAnnotation(Enumerated.class);
				if (enumerated == null) {
					try {
						enumerated = ReflectionUtils
								.getField(getter.getDeclaringClass(),
										(name.indexOf('.') > 0 ? name.substring(name.lastIndexOf('.') + 1) : name))
								.getAnnotation(Enumerated.class);
					} catch (NoSuchFieldException e) {
					}
				}
				Enum<?> en = ((Enum<?>) value);
				return enumerated != null && enumerated.value() == EnumType.STRING ? en.name() : en.ordinal();
			}
			return convertIfNessisary(value);
		} else {
			for (PropertyDescriptor pd : beanWrapper.getPropertyDescriptors()) {
				Enumerated enumerated = null;
				if (Enum.class.isAssignableFrom(pd.getPropertyType())) {
					enumerated = pd.getReadMethod().getAnnotation(Enumerated.class);
					if (enumerated == null) {
						try {
							enumerated = ReflectionUtils.getField(beanWrapper.getWrappedClass(), pd.getName())
									.getAnnotation(Enumerated.class);
						} catch (NoSuchFieldException e) {
						}
					}
				}
				Column column = pd.getReadMethod().getAnnotation(Column.class);
				if (column == null) {
					try {
						column = ReflectionUtils.getField(beanWrapper.getWrappedClass(), pd.getName())
								.getAnnotation(Column.class);
					} catch (NoSuchFieldException e) {
					}
				}
				if (column != null && name.equals(column.name())) {
					Object value = beanWrapper.getPropertyValue(pd.getName());
					if (value instanceof Enum) {
						Enum<?> en = ((Enum<?>) value);
						value = enumerated != null && enumerated.value() == EnumType.STRING ? en.name() : en.ordinal();
					}
					return convertIfNessisary(value);
				}
			}
			throw new IllegalArgumentException(
					"No such property '" + name + "' of bean " + beanWrapper.getWrappedClass().getName());
		}

	}

	private Object convertIfNessisary(Object value) {
		if (value instanceof Collection)
			return StringUtils.join((Collection<?>) value, ",");
		else if (value instanceof Object[])
			return StringUtils.join((Object[]) value, ",");
		else if (value instanceof Map)
			return JsonUtils.toJson(value);
		return value;
	}

}
