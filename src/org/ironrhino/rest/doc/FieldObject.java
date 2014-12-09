package org.ironrhino.rest.doc;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.rest.doc.annotation.Field;
import org.ironrhino.rest.doc.annotation.Fields;
import org.springframework.beans.BeanUtils;

public class FieldObject implements Serializable {

	private static final long serialVersionUID = -3390143355305565525L;

	private String name;

	private String type;

	private boolean required;

	private String label;

	private String description;

	private String defaultValue;

	private Map<String, String> values;

	public FieldObject() {

	}

	public FieldObject(String name, String type) {
		this.name = name;
		this.type = type;
	}

	public FieldObject(String name, String type, boolean required) {
		this(name, type);
		this.required = required;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public Map<String, String> getValues() {
		return values;
	}

	public void setValues(Map<String, String> values) {
		this.values = values;
	}

	public static FieldObject create(String name, Class<?> cls,
			boolean required, String defaultValue, Field fd) {
		String type = null;
		Map<String, String> values = null;
		if (cls.isEnum()) {
			type = "string";
			values = new LinkedHashMap<String, String>();
			try {
				Method m = cls.getMethod("values", new Class[0]);
				for (Enum<?> en : (Enum<?>[]) m.invoke(cls, new Object[0]))
					values.put(en.name(), en.toString());
			} catch (Exception e) {

			}
		} else {
			if (cls == Integer.class || cls == Integer.TYPE
					|| cls == Short.class || cls == Short.TYPE
					|| cls == Long.class || cls == Long.TYPE)
				type = "integer";
			else if (Number.class.isAssignableFrom(cls))
				type = "float";
			else if (cls == Boolean.class || cls == Boolean.TYPE)
				type = "boolean";
			else if (Date.class.isAssignableFrom(cls) || cls == String.class)
				type = "string";
			else
				type = "object";
		}
		FieldObject field = new FieldObject(name, type, required);
		if (StringUtils.isNotBlank(defaultValue))
			field.setDefaultValue(defaultValue);
		if (values != null)
			field.setValues(values);
		if (fd != null) {
			if (StringUtils.isNotBlank(fd.label()))
				field.setLabel(fd.label());
			if (StringUtils.isNotBlank(fd.description()))
				field.setDescription(fd.description());
		}
		return field;
	}

	public static List<FieldObject> from(Class<?> domainClass, Fields fields) {
		List<FieldObject> list = new ArrayList<>(fields.value().length);
		for (Field f : fields.value()) {
			String type = f.type();
			String name = f.name();
			if (StringUtils.isBlank(type)) {
				String[] arr = name.split("\\.");
				PropertyDescriptor pd = null;
				int i = 0;
				Class<?> clazz = domainClass;
				while (i < arr.length) {
					pd = BeanUtils.getPropertyDescriptor(clazz, arr[i]);
					if (pd == null)
						break;
					clazz = pd.getPropertyType();
					i++;
				}
				if (pd != null)
					list.add(create(name, pd.getPropertyType(), f.required(),
							f.defaultValue(), f));
			} else {
				FieldObject field = new FieldObject(name, type, f.required());
				if (StringUtils.isNotBlank(f.defaultValue()))
					field.setDefaultValue(f.defaultValue());
				if (StringUtils.isNotBlank(f.label()))
					field.setLabel(f.label());
				if (StringUtils.isNotBlank(f.description()))
					field.setDescription(f.description());
			}
		}
		return list;
	}

	public List<FieldObject> getList() {
		return Collections.emptyList();
	}

	public static void main(String[] args) throws Exception {
		Method m = FieldObject.class.getMethod("getList", new Class[0]);
		System.out.println(m.getReturnType().getClass());
		System.out.println(m.getGenericReturnType().getClass());
		
		if (m.getGenericReturnType() instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType)m.getGenericReturnType();
			System.out.println(pt.getActualTypeArguments()[0]);
		}
	}

}
