package org.ironrhino.core.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class JdbcRepositoryFactoryBean
		implements MethodInterceptor, FactoryBean<Object>, BeanFactoryAware, InitializingBean {

	private final Class<?> jdbcRepositoryClass;

	private final DatabaseProduct databaseProduct;

	private final int databaseMajorVersion;

	private final int databaseMinorVersion;

	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	private Object jdbcRepositoryBean;

	private Map<String, String> sqls;

	private BeanFactory beanFactory;

	private Partitioner defaultPartitioner;

	public JdbcRepositoryFactoryBean(Class<?> jdbcRepositoryClass, JdbcTemplate jdbcTemplate) {
		Assert.notNull(jdbcRepositoryClass, "jdbcRepositoryClass shouldn't be null");
		Assert.notNull(jdbcTemplate, "jdbcTemplate shouldn't be null");
		if (!jdbcRepositoryClass.isInterface())
			throw new IllegalArgumentException(jdbcRepositoryClass.getName() + " should be interface");
		this.jdbcRepositoryClass = jdbcRepositoryClass;
		this.jdbcRepositoryBean = new ProxyFactory(jdbcRepositoryClass, this)
				.getProxy(jdbcRepositoryClass.getClassLoader());
		this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
		try (Connection c = jdbcTemplate.getDataSource().getConnection()) {
			DatabaseMetaData dmd = c.getMetaData();
			databaseProduct = DatabaseProduct.parse(dmd.getDatabaseProductName());
			databaseMajorVersion = dmd.getDatabaseMajorVersion();
			databaseMinorVersion = dmd.getDatabaseMinorVersion();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public JdbcRepositoryFactoryBean(Class<?> jdbcRepositoryClass, DataSource dataSource) {
		this(jdbcRepositoryClass, new JdbcTemplate(dataSource));
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.sqls = loadSqls();
		Partition partition = jdbcRepositoryClass.getAnnotation(Partition.class);
		if (partition != null)
			defaultPartitioner = beanFactory.getBean(partition.partitioner());
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
	public Object invoke(MethodInvocation methodInvocation) {
		if (AopUtils.isToStringMethod(methodInvocation.getMethod())) {
			return "JdbcRepository for  [" + getObjectType().getName() + "]";
		}
		if (AppInfo.getStage() == Stage.DEVELOPMENT)
			this.sqls = loadSqls();
		Method method = methodInvocation.getMethod();
		String methodName = method.getName();
		String sql = sqls.get(methodName);
		if (StringUtils.isBlank(sql)) {
			Sql anno = AnnotationUtils.findAnnotation(method, Sql.class);
			if (anno != null)
				sql = anno.value();
		}
		if (StringUtils.isBlank(sql))
			throw new RuntimeException(
					"No sql found for method: " + jdbcRepositoryClass.getName() + "." + methodName + "()");
		SqlVerb sqlVerb = SqlVerb.parseBySql(sql);
		if (sqlVerb == null) {
			Transactional transactional = AnnotationUtils.findAnnotation(method, Transactional.class);
			if (transactional == null)
				transactional = AnnotationUtils.findAnnotation(jdbcRepositoryClass, Transactional.class);
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
				if (arg != null) {
					context.put(names[i], arg);
					if (arg instanceof Limiting) {
						sql = SqlUtils.appendLimitingClause(databaseProduct, databaseMajorVersion, databaseMinorVersion,
								sql, names[i], (Limiting) arg);
					}
					if (arg.getClass().isArray()) {
						Object[] objects = (Object[]) arg;
						sql = SqlUtils.expandCollectionParameter(sql, names[i], objects.length);
						if (objects.length > 0 && Enum.class.isAssignableFrom(arg.getClass().getComponentType())) {
							for (int j = 0; j < objects.length; j++)
								objects[j] = JdbcHelper.convertEnum(objects[j],
										methodInvocation.getMethod().getParameterAnnotations()[i]);
							arg = objects;

						}
					}
					if (arg instanceof Collection) {
						Collection<?> collection = (Collection<?>) arg;
						sql = SqlUtils.expandCollectionParameter(sql, names[i], collection.size());
						if (collection.size() > 0 && collection.iterator().next() instanceof Enum) {
							List<Object> objects = new ArrayList<>();
							for (Object obj : collection)
								objects.add(JdbcHelper.convertEnum(obj,
										methodInvocation.getMethod().getParameterAnnotations()[i]));
							arg = objects;
						}
					}
					if (arg instanceof Enum) {
						arg = JdbcHelper.convertEnum(arg, methodInvocation.getMethod().getParameterAnnotations()[i]);
					}
					sqlParameterSource.addValue(names[i], arg);
				}
			}
		}
		PartitionKey partitionKey = AnnotationUtils.findAnnotation(method, PartitionKey.class);
		if (partitionKey != null) {
			Partitioner partitioner = defaultPartitioner;
			Partition p = AnnotationUtils.findAnnotation(method, Partition.class);
			if (p != null)
				partitioner = beanFactory.getBean(p.partitioner());
			if (partitioner == null)
				throw new IllegalStateException("No partitioner found");
			String partition = partitioner.partition(ExpressionUtils.eval(partitionKey.value(), context));
			if (partition != null)
				context.put("PARTITION", partition);
		}
		if (!context.isEmpty() && (sql.indexOf('@') > -1))
			sql = ExpressionUtils.evalString(sql, context);

		Type returnType = method.getGenericReturnType();
		switch (sqlVerb) {
		case SELECT:
			if (returnType instanceof Class) {
				Class<?> clz = (Class<?>) returnType;
				if (BeanUtils.isSimpleValueType(clz)) {
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
						if (BeanUtils.isSimpleValueType(clz)) {
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

}
