package org.ironrhino.core.freemarker;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.ironrhino.core.struts.MyFreemarkerManager;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

public class ConstantsTemplateHashModel implements TemplateHashModel {

	@Override
	public TemplateModel get(String name) throws TemplateModelException {
		int index = name.lastIndexOf('.');
		if (index < 0)
			throw new TemplateModelException("Invalid constant expression: " + name);
		String className = name.substring(0, index);
		String fieldName = name.substring(index + 1);
		try {
			Class<?> clazz = Class.forName(className);
			Field field = clazz.getField(fieldName);
			if (!Modifier.isStatic(field.getModifiers()))
				throw new TemplateModelException(name + " is not static");
			Object value = field.get(null);
			if (value == null)
				return null;
			return MyFreemarkerManager.DEFAULT_BEANS_WRAPPER.wrap(value);
		} catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
			throw new TemplateModelException(e);
		}
	}

	@Override
	public boolean isEmpty() throws TemplateModelException {
		return false;
	}

}
