package org.ironrhino.core.struts.converter;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.util.BeanUtils;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.conversion.TypeConverter;
import com.opensymphony.xwork2.conversion.impl.XWorkConverter;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class MyXWorkConverter extends XWorkConverter {

	@Override
	public Object convertValue(Map<String, Object> context, Object target, Member member, String property, Object value,
			Class toClass) {
		if (Persistable.class.isAssignableFrom(toClass)) {
			String id = null;
			if (value instanceof String) {
				id = (String) value;
			} else if (value instanceof String[]) {
				String[] arr = (String[]) value;
				if (arr.length == 1)
					id = arr[0];
			}
			if (id == null || id.isEmpty())
				return null;
			Object entity;
			try {
				entity = toClass.getConstructor().newInstance();
				BeanUtils.setPropertyValue(entity, "id", id);
				return entity;
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
			}
		} else if ((Collection.class.isAssignableFrom(toClass) || toClass.isArray()) && value instanceof String[]
				&& target != null) {
			String[] arr = (String[]) value;
			if (arr.length == 1) {
				String s = arr[0];
				if (s == null)
					return null;
				if (s.startsWith("[") && s.endsWith("]"))
					s = s.substring(1, s.length() - 1);
				value = s.isEmpty() ? new String[0] : s.split(",\\s*");
			}
		}
		Object result = super.convertValue(context, target, member, property, value, toClass);
		if (TypeConverter.NO_CONVERSION_POSSIBLE.equals(result) && value != null) {
			if (File.class.isAssignableFrom(toClass) || Path.class.isAssignableFrom(toClass)
					|| toClass.getName().startsWith("java."))
				return result;
			try {
				Constructor ctor = toClass.getConstructor(value.getClass());
				result = ctor.newInstance(value);
				removeConversionError(context, property);
			} catch (Exception e) {
				if (value instanceof Object[]) {
					Object[] arr = (Object[]) value;
					if (arr.length == 1) {
						try {
							Constructor ctor = toClass.getConstructor(arr[0].getClass());
							result = ctor.newInstance(arr[0]);
							removeConversionError(context, property);
						} catch (Exception ex) {
						}
					}
				}
			}
		}
		return result;
	}

	protected void removeConversionError(Map<String, Object> context, String property) {
		if (context != null && (Boolean.TRUE.equals(context.get(REPORT_CONVERSION_ERRORS)))) {
			String realProperty = property;
			String fullName = (String) context.get(CONVERSION_PROPERTY_FULLNAME);
			if (fullName != null)
				realProperty = fullName;
			Map<String, Object> conversionErrors = (Map<String, Object>) context.get(ActionContext.CONVERSION_ERRORS);
			if (conversionErrors != null)
				conversionErrors.remove(realProperty);
		}
	}

}
