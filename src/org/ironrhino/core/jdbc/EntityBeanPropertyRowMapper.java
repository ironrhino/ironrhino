package org.ironrhino.core.jdbc;

import java.beans.PropertyDescriptor;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.persistence.Column;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.spring.converter.CustomConversionService;
import org.ironrhino.core.util.BeanUtils;
import org.ironrhino.core.util.ReflectionUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.jdbc.support.JdbcUtils;

public class EntityBeanPropertyRowMapper<T> extends MyBeanPropertyRowMapper<T> {

	private final BeanWrapper beanWrapper;

	public EntityBeanPropertyRowMapper() {
		setConversionService(CustomConversionService.getSharedInstance());
		beanWrapper = null;
	}

	public EntityBeanPropertyRowMapper(Class<T> mappedClass) {
		setConversionService(CustomConversionService.getSharedInstance());
		beanWrapper = new BeanWrapperImpl(mappedClass);
		initialize(mappedClass);
	}

	@Override
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
				name = lowerCaseName(column.name());
		}
		return super.underscoreName(name);
	}

	@Override
	protected Object getColumnValue(ResultSet rs, int index, PropertyDescriptor pd) throws SQLException {
		if (!org.springframework.beans.BeanUtils.isSimpleValueType(pd.getPropertyType()))
			return JdbcUtils.getResultSetValue(rs, index, null);
		return super.getColumnValue(rs, index, pd);
	}

	@Override
	protected PropertyDescriptor tryFindPropertyDescriptor(String column, BeanWrapper bw) {
		if (column.indexOf('.') > 0) {
			BeanUtils.createParentIfNull(bw.getWrappedInstance(), column);
			return bw.getPropertyDescriptor(column);
		}
		return null;
	}

}
