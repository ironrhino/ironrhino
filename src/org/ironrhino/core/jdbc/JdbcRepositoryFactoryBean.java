package org.ironrhino.core.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EnumType;
import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilderFactory;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.AppInfo.Stage;
import org.ironrhino.core.util.ExpressionUtils;
import org.ironrhino.core.util.ReflectionUtils;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class JdbcRepositoryFactoryBean implements MethodInterceptor, FactoryBean<Object> {

	private final Class<?> jdbcRepositoryClass;

	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	private final DatabaseProduct databaseProduct;

	private Object jdbcRepositoryBean;

	private Map<String, String> sqls;

	public JdbcRepositoryFactoryBean(Class<?> jdbcRepositoryClass, DataSource dataSource) {
		Assert.notNull(jdbcRepositoryClass);
		Assert.notNull(dataSource);
		if (!jdbcRepositoryClass.isInterface())
			throw new IllegalArgumentException(jdbcRepositoryClass.getName() + " should be interface");
		this.jdbcRepositoryClass = jdbcRepositoryClass;
		this.jdbcRepositoryBean = new ProxyFactory(jdbcRepositoryClass, this)
				.getProxy(jdbcRepositoryClass.getClassLoader());
		try (Connection c = dataSource.getConnection()) {
			databaseProduct = DatabaseProduct.parse(c.getMetaData().getDatabaseProductName());
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
		this.sqls = loadSqls();
	}

	private Map<String, String> loadSqls() {
		Map<String, String> map = new HashMap<>();
		Properties props = new Properties();
		try (InputStream is = jdbcRepositoryClass
				.getResourceAsStream(jdbcRepositoryClass.getSimpleName() + ".properties")) {
			if (is != null)
				props.load(is);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		try (InputStream is = jdbcRepositoryClass.getResourceAsStream(
				jdbcRepositoryClass.getSimpleName() + "." + databaseProduct.name() + ".properties")) {
			if (is != null)
				props.load(is);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		for (Map.Entry<Object, Object> entry : props.entrySet()) {
			map.put(entry.getKey().toString(), entry.getValue().toString());
		}
		try (InputStream is = jdbcRepositoryClass.getResourceAsStream(jdbcRepositoryClass.getSimpleName() + ".xml")) {
			if (is != null) {
				NodeList nl = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is)
						.getElementsByTagName("entry");
				for (int i = 0; i < nl.getLength(); i++) {
					Element entry = (Element) nl.item(i);
					map.put(entry.getAttribute("key"), entry.getTextContent());
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		try (InputStream is = jdbcRepositoryClass
				.getResourceAsStream(jdbcRepositoryClass.getSimpleName() + "." + databaseProduct.name() + ".xml")) {
			if (is != null) {
				NodeList nl = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is)
						.getElementsByTagName("entry");
				for (int i = 0; i < nl.getLength(); i++) {
					Element entry = (Element) nl.item(i);
					map.put(entry.getAttribute("key"), entry.getTextContent());
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return map;
	}

	@Override
	public Object getObject() throws Exception {
		return jdbcRepositoryBean;
	}

	@Override
	public Class<?> getObjectType() {
		return jdbcRepositoryClass;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public Object invoke(MethodInvocation methodInvocation) throws Throwable {
		if (AopUtils.isToStringMethod(methodInvocation.getMethod())) {
			return "JdbcRepository for  [" + getObjectType().getName() + "]";
		}
		if (AppInfo.getStage() == Stage.DEVELOPMENT)
			this.sqls = loadSqls();
		Method method = methodInvocation.getMethod();
		String methodName = method.getName();
		String sql = sqls.get(methodName);
		if (StringUtils.isBlank(sql)) {
			Sql anno = method.getAnnotation(Sql.class);
			if (anno != null)
				sql = anno.value();
		}
		if (StringUtils.isBlank(sql))
			throw new RuntimeException(
					"No sql found for method: " + jdbcRepositoryClass.getName() + "." + methodName + "()");
		SqlVerb sqlVerb = SqlVerb.parseBySql(sql);
		if (sqlVerb == null) {
			Transactional transactional = method.getAnnotation(Transactional.class);
			if (transactional == null)
				transactional = method.getDeclaringClass().getAnnotation(Transactional.class);
			if (transactional != null && transactional.readOnly()) {
				sqlVerb = SqlVerb.SELECT;
			}
			if (sqlVerb == null) {
				if (methodName.startsWith("get") || methodName.startsWith("load") || methodName.startsWith("query")
						|| methodName.startsWith("find") || methodName.startsWith("search")
						|| methodName.startsWith("list") || methodName.startsWith("count"))
					sqlVerb = SqlVerb.SELECT;
			}
		}
		if (sqlVerb == null)
			throw new IllegalArgumentException("Invalid sql: " + sql);

		Object[] arguments = methodInvocation.getArguments();
		NestedPathMapSqlParameterSource sqlParameterSource = new NestedPathMapSqlParameterSource();
		Map<String, Object> context = new HashMap<>();
		if (arguments.length > 0) {
			String[] names = ReflectionUtils.getParameterNames(methodInvocation.getMethod());
			if (names == null)
				throw new RuntimeException("No parameter names discovered for method, please consider using @Param");
			for (int i = 0; i < names.length; i++) {
				Object arg = arguments[i];
				context.put(names[i], arg);
				if (arg != null) {
					if (arg.getClass().isArray()) {
						Object[] objects = (Object[]) arg;
						sql = expandSql(sql, names[i], objects.length);
						if (objects.length > 0 && Enum.class.isAssignableFrom(arg.getClass().getComponentType())) {
							for (int j = 0; j < objects.length; j++)
								objects[j] = convertEnum(objects[j],
										methodInvocation.getMethod().getParameterAnnotations()[i]);
							arg = objects;

						}
					}
					if (arg instanceof Collection) {
						Collection<?> collection = (Collection<?>) arg;
						sql = expandSql(sql, names[i], collection.size());
						if (collection.size() > 0 && collection.iterator().next() instanceof Enum) {
							List<Object> objects = new ArrayList<>();
							for (Object obj : collection)
								objects.add(
										convertEnum(obj, methodInvocation.getMethod().getParameterAnnotations()[i]));
							arg = objects;
						}
					}
					if (arg instanceof Enum) {
						arg = convertEnum(arg, methodInvocation.getMethod().getParameterAnnotations()[i]);
					}
				}
				sqlParameterSource.addValue(names[i], arg);
			}
		}
		if (!context.isEmpty() && sql.indexOf('@') > -1)
			sql = ExpressionUtils.evalString(sql, context);

		Type returnType = method.getGenericReturnType();
		switch (sqlVerb) {
		case SELECT:
			if (returnType instanceof Class) {
				Class<?> clz = (Class<?>) returnType;
				if (isScalar(clz)) {
					return namedParameterJdbcTemplate.queryForObject(sql, sqlParameterSource, clz);
				} else {
					List<?> result = namedParameterJdbcTemplate.query(sql, sqlParameterSource,
							new EntityBeanPropertyRowMapper<>(clz));
					if (result.size() > 1)
						throw new RuntimeException("Incorrect result size: expected 1, actual " + result.size());
					return result.isEmpty() ? null : result.get(0);
				}
			} else if (returnType instanceof ParameterizedType) {
				ParameterizedType pt = (ParameterizedType) returnType;
				if (pt.getRawType() == List.class || pt.getRawType() == Collection.class) {
					Type type = pt.getActualTypeArguments()[0];
					if (type instanceof Class) {
						Class<?> clz = (Class<?>) type;
						if (isScalar(clz)) {
							return namedParameterJdbcTemplate.queryForList(sql, sqlParameterSource, clz);
						} else {
							return namedParameterJdbcTemplate.query(sql, sqlParameterSource,
									new EntityBeanPropertyRowMapper<>(clz));
						}
					}
				}
			}
			throw new UnsupportedOperationException("Unsupported return type: " + returnType.getTypeName());
		default:
			int rows = namedParameterJdbcTemplate.update(sql, sqlParameterSource);
			if (returnType == void.class) {
				return null;
			} else if (returnType == int.class) {
				return rows;
			} else {
				throw new UnsupportedOperationException("Unsupported return type: " + returnType.getTypeName());
			}
		}

	}

	private static String expandSql(String sql, String paramName, int size) {
		if (size < 1 || size > 100)
			throw new IllegalArgumentException("invalid size: " + size);
		StringBuilder sb = new StringBuilder();
		sb.append('(');
		for (int i = 0; i < size; i++) {
			sb.append(":").append(paramName).append('[').append(i).append(']');
			if (i != size - 1)
				sb.append(',');
		}
		sb.append(')');
		String regex = "\\(\\s*:" + paramName + "\\s*\\)";
		return sql.replaceAll(regex, sb.toString());
	}

	private static Object convertEnum(Object arg, Annotation[] paramAnnotations) {
		Enum<?> en = (Enum<?>) arg;
		for (Annotation ann : paramAnnotations) {
			if (ann instanceof Enumerated) {
				arg = (((Enumerated) ann).value() == EnumType.ORDINAL) ? en.ordinal() : en.name();
				break;
			}
			if (ann instanceof javax.persistence.Enumerated) {
				arg = (((javax.persistence.Enumerated) ann).value() == EnumType.ORDINAL) ? en.ordinal() : en.name();
				break;
			}
		}
		return arg;
	}

	private static boolean isScalar(Class<?> type) {
		if ((Boolean.TYPE == type) || (Boolean.class == type))
			return true;
		if ((Byte.TYPE == type) || (Byte.class == type))
			return true;
		if ((Short.TYPE == type) || (Short.class == type))
			return true;
		if ((Integer.TYPE == type) || (Integer.class == type))
			return true;
		if ((Long.TYPE == type) || (Long.class == type))
			return true;
		if ((Float.TYPE == type) || (Float.class == type))
			return true;
		if ((Double.TYPE == type) || (Double.class == type) || (Number.class == type))
			return true;
		if (BigDecimal.class == type)
			return true;
		if (java.sql.Date.class == type)
			return true;
		if (Time.class == type)
			return true;
		if ((Timestamp.class == type) || (java.util.Date.class == type))
			return true;
		return false;
	}

}
