package org.ironrhino.core.jdbc;

import java.beans.PropertyDescriptor;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.NotWritablePropertyException;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/*
 * @see https://jira.spring.io/browse/SPR-15004
 */
class MyBeanPropertyRowMapper<T> implements RowMapper<T> {
	protected final Log logger = LogFactory.getLog(getClass());

	private Class<T> mappedClass;

	private boolean checkFullyPopulated = false;

	private boolean primitivesDefaultedForNullValue = false;

	private ConversionService conversionService = new DefaultConversionService();

	private Map<String, PropertyDescriptor> mappedFields;

	private Set<String> mappedProperties;

	public MyBeanPropertyRowMapper() {
	}

	public MyBeanPropertyRowMapper(Class<T> mappedClass) {
		initialize(mappedClass);
	}

	public MyBeanPropertyRowMapper(Class<T> mappedClass, boolean checkFullyPopulated) {
		initialize(mappedClass);
		this.checkFullyPopulated = checkFullyPopulated;
	}

	public void setMappedClass(Class<T> mappedClass) {
		if (this.mappedClass == null) {
			initialize(mappedClass);

		} else if (this.mappedClass != mappedClass) {
			throw new InvalidDataAccessApiUsageException("The mapped class can not be reassigned to map to "
					+ mappedClass + " since it is already providing mapping for " + this.mappedClass);
		}
	}

	public final Class<T> getMappedClass() {
		return mappedClass;
	}

	public void setCheckFullyPopulated(boolean checkFullyPopulated) {
		this.checkFullyPopulated = checkFullyPopulated;
	}

	public boolean isCheckFullyPopulated() {
		return checkFullyPopulated;
	}

	public void setPrimitivesDefaultedForNullValue(boolean primitivesDefaultedForNullValue) {
		this.primitivesDefaultedForNullValue = primitivesDefaultedForNullValue;
	}

	public boolean isPrimitivesDefaultedForNullValue() {
		return primitivesDefaultedForNullValue;
	}

	public void setConversionService(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	public ConversionService getConversionService() {
		return conversionService;
	}

	protected void initialize(Class<T> mappedClass) {
		this.mappedClass = mappedClass;
		mappedFields = new HashMap<>();
		mappedProperties = new HashSet<>();
		PropertyDescriptor[] pds = BeanUtils.getPropertyDescriptors(mappedClass);
		for (PropertyDescriptor pd : pds) {
			if (pd.getWriteMethod() != null) {
				mappedFields.put(lowerCaseName(pd.getName()), pd);
				String underscoredName = underscoreName(pd.getName());
				if (!lowerCaseName(pd.getName()).equals(underscoredName)) {
					mappedFields.put(underscoredName, pd);
				}
				mappedProperties.add(pd.getName());
			}
		}
	}

	protected String underscoreName(String name) {
		if (!StringUtils.hasLength(name)) {
			return "";
		}
		StringBuilder result = new StringBuilder();
		result.append(lowerCaseName(name.substring(0, 1)));
		for (int i = 1; i < name.length(); i++) {
			String s = name.substring(i, i + 1);
			String slc = lowerCaseName(s);
			if (!s.equals(slc)) {
				result.append("_").append(slc);
			} else {
				result.append(s);
			}
		}
		return result.toString();
	}

	protected String lowerCaseName(String name) {
		return name.toLowerCase(Locale.US);
	}

	@Override
	public T mapRow(ResultSet rs, int rowNumber) throws SQLException {
		Assert.state(mappedClass != null, "Mapped class was not specified");
		T mappedObject = BeanUtils.instantiateClass(mappedClass);
		BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(mappedObject);
		initBeanWrapper(bw);

		ResultSetMetaData rsmd = rs.getMetaData();
		int columnCount = rsmd.getColumnCount();
		Set<String> populatedProperties = isCheckFullyPopulated() ? new HashSet<>() : null;

		for (int index = 1; index <= columnCount; index++) {
			String column = JdbcUtils.lookupColumnName(rsmd, index);
			String field = lowerCaseName(column.replaceAll(" ", ""));
			PropertyDescriptor pd = mappedFields.get(field);
			boolean fromFind = false;
			if (pd == null) {
				pd = tryFindPropertyDescriptor(column, bw);
				fromFind = true;
			}
			if (pd != null) {
				try {
					Object value = getColumnValue(rs, index, pd);
					if ((rowNumber == 0) && (logger.isDebugEnabled())) {
						logger.debug("Mapping column '" + column + "' to property '" + pd.getName() + "' of type '"
								+ ClassUtils.getQualifiedName(pd.getPropertyType()) + "'");
					}
					try {
						if (fromFind) {
							bw.setPropertyValue(column, value);
						} else {
							if (value == null || pd.getPropertyType().isAssignableFrom(value.getClass())) {
								try {
									// https://github.com/spring-projects/spring-framework/blob/098ac0bbb88cd178e85b7dc31642bed091560316/spring-core/src/main/java/org/springframework/core/convert/TypeDescriptor.java#L501
									// Annotation.equals is expensive cause bw.setPropertyValue() is costly
									// if ConversionService is present for massive fields with heavy annotation
									// use reflection directly
									pd.getWriteMethod().invoke(mappedObject, value);
								} catch (Exception e) {
									bw.setPropertyValue(pd.getName(), value);
								}
							} else {
								bw.setPropertyValue(pd.getName(), value);
							}
						}
					} catch (TypeMismatchException ex) {
						if ((value == null) && (primitivesDefaultedForNullValue)) {
							if (logger.isDebugEnabled()) {
								logger.debug("Intercepted TypeMismatchException for row " + rowNumber + " and column '"
										+ column + "' with null value when setting property '" + pd

												.getName()
										+ "' of type '" + ClassUtils.getQualifiedName(pd.getPropertyType())
										+ "' on object: " + mappedObject, ex);
							}

						} else {
							throw ex;
						}
					}
					if (populatedProperties != null) {
						populatedProperties.add(pd.getName());
					}
				} catch (NotWritablePropertyException ex) {
					throw new DataRetrievalFailureException(
							"Unable to map column '" + column + "' to property '" + pd.getName() + "'", ex);

				}

			} else if ((rowNumber == 0) && (logger.isDebugEnabled())) {
				logger.debug("No property found for column '" + column + "' mapped to field '" + field + "'");
			}
		}

		if ((populatedProperties != null) && (!populatedProperties.equals(mappedProperties))) {
			throw new InvalidDataAccessApiUsageException(
					"Given ResultSet does not contain all fields necessary to populate object of class ["
							+ mappedClass.getName() + "]: " + mappedProperties);
		}

		return mappedObject;
	}

	protected void initBeanWrapper(BeanWrapper bw) {
		ConversionService cs = getConversionService();
		if (cs != null) {
			bw.setConversionService(cs);
		}
	}

	protected Object getColumnValue(ResultSet rs, int index, PropertyDescriptor pd) throws SQLException {
		return JdbcUtils.getResultSetValue(rs, index, pd.getPropertyType());
	}

	protected PropertyDescriptor tryFindPropertyDescriptor(String column, BeanWrapper bw) {
		return null;
	}

	public static <T> MyBeanPropertyRowMapper<T> newInstance(Class<T> mappedClass) {
		return new MyBeanPropertyRowMapper<T>(mappedClass);
	}
}
