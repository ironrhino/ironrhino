package org.ironrhino.rest.doc;

import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.NaturalId;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.util.ReflectionUtils;
import org.ironrhino.rest.doc.annotation.Field;
import org.ironrhino.rest.doc.annotation.Fields;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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

	public static FieldObject create(String name, Class<?> cls, boolean required, String defaultValue, Field fd) {
		String type = null;
		Map<String, String> values = null;
		if (cls.isEnum()) {
			type = "string";
			values = new LinkedHashMap<String, String>();
			try {
				for (Object o : cls.getEnumConstants()) {
					Enum<?> en = (Enum<?>) o;
					values.put(en.name(), en.toString());
				}
			} catch (Exception e) {

			}
		} else {
			if (cls == Integer.class || cls == Integer.TYPE || cls == Short.class || cls == Short.TYPE)
				type = "int";
			else if (cls == Long.class || cls == Long.TYPE)
				type = "long";
			else if (Number.class.isAssignableFrom(cls))
				type = "float";
			else if (cls == Boolean.class || cls == Boolean.TYPE)
				type = "boolean";
			else if (Collection.class.isAssignableFrom(cls))
				type = "array";
			else if (java.sql.Timestamp.class.isAssignableFrom(cls))
				type = "timestamp";
			else if (Date.class.isAssignableFrom(cls))
				type = "date";
			else if (cls == String.class)
				type = "string";
			else if (File.class.isAssignableFrom(cls) || MultipartFile.class.isAssignableFrom(cls))
				type = "file";
			else
				type = "object";
		}
		FieldObject field = new FieldObject(name, type, required);
		if (StringUtils.isNotBlank(defaultValue) && !ValueConstants.DEFAULT_NONE.equals(defaultValue))
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

	public static List<FieldObject> createList(Class<?> domainClass, Fields fields, boolean forRequest) {
		if (fields != null && fields.value().length > 0) {
			List<FieldObject> list = new ArrayList<>(fields.value().length);
			for (Field f : fields.value()) {
				String type = f.type();
				String name = f.name();
				if (StringUtils.isBlank(type)) {
					PropertyDescriptor pd = org.ironrhino.core.util.BeanUtils.getPropertyDescriptor(domainClass, name);
					if (pd != null) {
						list.add(create(name, pd.getPropertyType(), f.required(), f.defaultValue(), f));
						continue;
					} else {
						type = "string";
					}
				}
				FieldObject field = new FieldObject(name, type, f.required());
				if (StringUtils.isNotBlank(f.defaultValue()) && !ValueConstants.DEFAULT_NONE.equals(f.defaultValue()))
					field.setDefaultValue(f.defaultValue());
				if (StringUtils.isNotBlank(f.label()))
					field.setLabel(f.label());
				if (StringUtils.isNotBlank(f.description()))
					field.setDescription(f.description());
				list.add(field);
			}
			return list;
		} else {
			List<FieldObject> list = new ArrayList<>();
			final List<String> fieldNames = ReflectionUtils.getAllFields(domainClass);
			JsonIgnoreProperties jip = domainClass.getAnnotation(JsonIgnoreProperties.class);
			List<String> ignoreList = new ArrayList<>();
			if (jip != null)
				ignoreList.addAll(Arrays.asList(jip.value()));
			PropertyDescriptor[] pds = BeanUtils.getPropertyDescriptors(domainClass);
			for (PropertyDescriptor pd : pds) {
				String name = pd.getName();
				if (name.equals("class") || ignoreList.contains(name))
					continue;
				if (forRequest && (pd.getReadMethod() == null || pd.getWriteMethod() == null
						|| pd.getWriteMethod().getAnnotation(JsonIgnore.class) != null))
					continue;
				if (!forRequest
						&& (pd.getReadMethod() == null || pd.getReadMethod().getAnnotation(JsonIgnore.class) != null))
					continue;
				boolean required = false;
				try {
					java.lang.reflect.Field f = pd.getReadMethod().getDeclaringClass().getDeclaredField(name);
					if (f.getAnnotation(JsonIgnore.class) != null)
						continue;

					if (Persistable.class.isAssignableFrom(domainClass)) {
						if (forRequest) {
							if ("id".equals(name))
								continue;

						} else {
							if ("id".equals(name))
								required = true;
							if (f.getAnnotation(NaturalId.class) != null)
								required = true;
						}
					}

					if (!required) {
						UiConfig uic = null;
						if (pd.getReadMethod() != null) {
							uic = pd.getReadMethod().getAnnotation(UiConfig.class);
						}
						if (uic == null)
							uic = f.getAnnotation(UiConfig.class);
						if (uic != null && uic.required())
							required = true;
					}
				} catch (NoSuchFieldException e) {
					continue;
				} catch (Exception e) {
					e.printStackTrace();
				}
				list.add(create(name, pd.getPropertyType(), required, null, null));
			}
			Collections.sort(list, (o1, o2) -> {
				return fieldNames.indexOf(o1.getName()) - fieldNames.indexOf(o2.getName());
			});
			return list;
		}
	}

}
