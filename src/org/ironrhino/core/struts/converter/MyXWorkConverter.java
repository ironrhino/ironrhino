package org.ironrhino.core.struts.converter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.util.Map;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.conversion.TypeConverter;
import com.opensymphony.xwork2.conversion.impl.XWorkConverter;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class MyXWorkConverter extends XWorkConverter {

	@Override
	public Object convertValue(Map<String, Object> context, Object target, Member member, String property, Object value,
			Class toClass) {
		Object result = super.convertValue(context, target, member, property, value, toClass);
		if (TypeConverter.NO_CONVERSION_POSSIBLE.equals(result) && value != null) {
			try {
				Constructor ctor = toClass.getConstructor(value.getClass());
				result = ctor.newInstance(value);
				removeConversionError(context, property);
			} catch (Exception e) {
				if (value.getClass().isArray()) {
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
