package org.ironrhino.core.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.AppInfo.Stage;
import org.ironrhino.core.util.ReflectionUtils;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.Assert;

public class JdbcRepositoryFactoryBean implements MethodInterceptor, FactoryBean<Object> {

	private final Class<?> jdbcRepositoryClass;

	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	private final DatabaseProduct databaseProduct;

	private Object jdbcRepositoryBean;

	private Properties sqls;

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

	private Properties loadSqls() {
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
		return props;
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
		String sql = sqls.getProperty(methodName);
		if (StringUtils.isBlank(sql))
			throw new RuntimeException(
					"No sql found for method: " + jdbcRepositoryClass.getName() + "." + methodName + "()");
		SqlVerb sqlVerb = SqlVerb.parseBySql(sql);
		String[] names = ReflectionUtils.getParameterNames(method);
		Object[] arguments = methodInvocation.getArguments();
		Map<String, Object> paramMap = new HashMap<>();
		for (int i = 0; i < names.length; i++)
			paramMap.put(names[i], arguments[i]);
		Type returnType = method.getGenericReturnType();
		switch (sqlVerb) {
		case SELECT:
			if (returnType instanceof Class) {
				Class<?> clz = (Class<?>) returnType;
				List<?> result = namedParameterJdbcTemplate.query(sql, new NestedPathMapSqlParameterSource(paramMap),
						new EntityBeanPropertyRowMapper<>(clz));
				if (result.size() > 1)
					throw new RuntimeException("Incorrect result size: expected 1, actual " + result.size());
				return result.isEmpty() ? null : result.get(0);
			} else if (returnType instanceof ParameterizedType) {
				ParameterizedType pt = (ParameterizedType) returnType;
				if (pt.getRawType() == List.class || pt.getRawType() == Collection.class) {
					Type type = pt.getActualTypeArguments()[0];
					if (type instanceof Class) {
						Class<?> clz = (Class<?>) type;
						return namedParameterJdbcTemplate.query(sql, new NestedPathMapSqlParameterSource(paramMap),
								new EntityBeanPropertyRowMapper<>(clz));
					}
				}
			}
			throw new UnsupportedOperationException("Unsupported return type: " + returnType.getTypeName());
		default:
			int rows = namedParameterJdbcTemplate.update(sql, new NestedPathMapSqlParameterSource(paramMap));
			if (returnType == void.class) {
				return null;
			} else if (returnType == int.class) {
				return rows;
			} else {
				throw new UnsupportedOperationException("Unsupported return type: " + returnType.getTypeName());
			}
		}

	}

}
