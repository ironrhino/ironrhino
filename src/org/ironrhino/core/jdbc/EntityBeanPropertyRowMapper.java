package org.ironrhino.core.jdbc;

import java.beans.PropertyDescriptor;

import javax.persistence.Column;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.ReflectionUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.jdbc.core.BeanPropertyRowMapper;

public class EntityBeanPropertyRowMapper<T> extends BeanPropertyRowMapper<T> {

	static DefaultConversionService conversionService = new DefaultConversionService();

	private final BeanWrapper beanWrapper;

	public EntityBeanPropertyRowMapper() {
		beanWrapper = null;
	}

	public EntityBeanPropertyRowMapper(Class<T> mappedClass) {
		initialize(mappedClass);
		beanWrapper = new BeanWrapperImpl(mappedClass);
	}

	@Override
	protected void initBeanWrapper(BeanWrapper bw) {
		bw.setConversionService(conversionService);
	}

	protected String underscoreName(String name) {
		if (beanWrapper != null) {
			PropertyDescriptor pd = beanWrapper.getPropertyDescriptor(name);
			Column column = pd.getReadMethod().getAnnotation(Column.class);
			if (column == null) {
				try {
					column = ReflectionUtils.getField(beanWrapper.getWrappedClass(), pd.getName())
							.getAnnotation(Column.class);
				} catch (NoSuchFieldException e) {
				}
			}
			if (column != null && StringUtils.isNotBlank(column.name()))
				return column.name();
		}
		return super.underscoreName(name);
	}

}
