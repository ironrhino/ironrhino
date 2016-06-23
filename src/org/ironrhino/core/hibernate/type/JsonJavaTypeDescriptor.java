package org.ironrhino.core.hibernate.type;

import java.io.IOException;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;
import org.hibernate.type.descriptor.java.MutableMutabilityPlan;
import org.hibernate.usertype.DynamicParameterizedType;
import org.ironrhino.core.util.JsonUtils;
import org.springframework.beans.BeanUtils;

public class JsonJavaTypeDescriptor extends AbstractTypeDescriptor<Object> implements DynamicParameterizedType {

	private static final long serialVersionUID = -6335930102166043485L;

	private Class<?> clazz;

	@Override
	public void setParameterValues(Properties parameters) {
		clazz = ((ParameterType) parameters.get(PARAMETER_TYPE)).getReturnedClass();
	}

	public JsonJavaTypeDescriptor() {
		super(Object.class, new MutableMutabilityPlan<Object>() {

			private static final long serialVersionUID = 1940316475848878030L;

			@Override
			protected Object deepCopyNotNull(Object value) {
				Object obj;
				try {
					obj = value.getClass().newInstance();
					BeanUtils.copyProperties(value, obj);
					return obj;
				} catch (InstantiationException | IllegalAccessException e) {
					throw new RuntimeException(e);
				}

			}
		});
	}

	@Override
	public boolean areEqual(Object value, Object another) {
		if (value == another)
			return true;
		if (value == null || another == null)
			return false;
		return JsonUtils.toJson(value).equals(JsonUtils.toJson(another));
	}

	@Override
	public String toString(Object value) {
		return JsonUtils.toJson(value);
	}

	@Override
	public Object fromString(String string) {
		if (StringUtils.isBlank(string))
			return null;
		try {
			return JsonUtils.fromJson(string, clazz);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public <X> X unwrap(Object value, Class<X> type, WrapperOptions options) {
		if (value == null)
			return null;
		if (String.class == type)
			return (X) toString(value);
		throw unknownUnwrap(type);
	}

	@Override
	public <X> Object wrap(X value, WrapperOptions options) {
		if (value == null)
			return null;
		return fromString(value.toString());
	}

}
