package org.ironrhino.rest.model;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.rest.annotation.Field;
import org.ironrhino.rest.annotation.Fields;
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
		String type;
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
			type = cls.getSimpleName().toLowerCase();
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
			Method m = BeanUtils.getPropertyDescriptor(domainClass, f.name())
					.getReadMethod();
			if (m == null)
				continue;
			list.add(create(f.name(), m.getReturnType(), f.required(),
					f.defaultValue(), f));
		}
		return list;
	}

	@Override
	public String toString() {
		return "Field [name=" + name + ", type=" + type + ", required="
				+ required + ", label=" + label + ", description="
				+ description + ", defaultValue=" + defaultValue + ", values="
				+ values + "]";
	}

}
