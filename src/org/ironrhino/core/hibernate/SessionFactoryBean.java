package org.ironrhino.core.hibernate;

import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import javax.persistence.Entity;
import javax.sql.DataSource;
import javax.validation.ValidatorFactory;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Interceptor;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.SessionFactory;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostLoadEventListener;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.event.spi.PreDeleteEventListener;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.ironrhino.core.hibernate.dialect.MyDialectResolver;
import org.ironrhino.core.hibernate.event.DeleteCallbackEventListener;
import org.ironrhino.core.hibernate.event.FlushEntityCallbackEventListener;
import org.ironrhino.core.hibernate.event.MergeCallbackEventListener;
import org.ironrhino.core.hibernate.event.PersistCallbackEventListener;
import org.ironrhino.core.hibernate.event.PostDeleteCallbackEventListener;
import org.ironrhino.core.hibernate.event.PostInsertCallbackEventListener;
import org.ironrhino.core.hibernate.event.PostLoadCallbackEventListener;
import org.ironrhino.core.hibernate.event.PostUpdateCallbackEventListener;
import org.ironrhino.core.hibernate.event.SaveCallbackEventListener;
import org.ironrhino.core.hibernate.event.SaveOrUpdateCallbackEventListener;
import org.ironrhino.core.hibernate.type.YearMonthType;
import org.ironrhino.core.jdbc.DatabaseProduct;
import org.ironrhino.core.spring.DefaultPropertiesProvider;
import org.ironrhino.core.util.ClassScanner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.Environment;
import org.springframework.orm.hibernate5.LocalSessionFactoryBuilder;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SessionFactoryBean extends org.springframework.orm.hibernate5.LocalSessionFactoryBean
		implements DefaultPropertiesProvider {

	@Autowired
	private Environment environment;

	@Autowired(required = false)
	private List<StandardServiceInitiator<?>> standardServiceInitiators;

	@Autowired(required = false)
	private List<AttributeConverter<?, ?>> attributeConverters;

	@Autowired(required = false)
	private ImplicitNamingStrategy implicitNamingStrategy;

	@Autowired(required = false)
	private PhysicalNamingStrategy physicalNamingStrategy;

	@Autowired(required = false)
	private MultiTenantConnectionProvider multiTenantConnectionProvider;

	@Autowired(required = false)
	private CurrentTenantIdentifierResolver currentTenantIdentifierResolver;

	@Autowired(required = false)
	private Interceptor entityInterceptor;

	@Autowired(required = false)
	private PreInsertEventListener[] preInsertEventListeners = new PreInsertEventListener[0];

	@Autowired(required = false)
	private PreUpdateEventListener[] preUpdateEventListeners = new PreUpdateEventListener[0];

	@Autowired(required = false)
	private PreDeleteEventListener[] preDeleteEventListeners = new PreDeleteEventListener[0];

	@Autowired(required = false)
	private PostInsertEventListener[] postInsertEventListeners = new PostInsertEventListener[0];

	@Autowired(required = false)
	private PostUpdateEventListener[] postUpdateEventListeners = new PostUpdateEventListener[0];

	@Autowired(required = false)
	private PostDeleteEventListener[] postDeleteEventListeners = new PostDeleteEventListener[0];

	@Autowired(required = false)
	private PostLoadEventListener[] postLoadEventListeners = new PostLoadEventListener[0];

	@Autowired
	private ValidatorFactory validatorFactory;

	private Class<?>[] annotatedClasses;

	private String excludeFilter;

	private DataSource dataSource;

	private final Map<String, String> defaultProperties;

	public SessionFactoryBean() {
		// default properties override hibernate defaults
		Map<String, String> map = new HashMap<>();
		map.put(AvailableSettings.DIALECT_RESOLVERS, MyDialectResolver.class.getName());
		map.put(AvailableSettings.MAX_FETCH_DEPTH, String.valueOf(3));
		map.put(AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, String.valueOf(10));
		map.put(AvailableSettings.STATEMENT_FETCH_SIZE, String.valueOf(20));
		map.put(AvailableSettings.STATEMENT_BATCH_SIZE, String.valueOf(50));
		map.put(AvailableSettings.ORDER_INSERTS, String.valueOf(true));
		map.put(AvailableSettings.ORDER_UPDATES, String.valueOf(true));
		map.put(AvailableSettings.KEYWORD_AUTO_QUOTING_ENABLED, String.valueOf(true));
		map.put(AvailableSettings.USE_SECOND_LEVEL_CACHE, String.valueOf(false));
		map.put(AvailableSettings.HBM2DDL_AUTO, "update");
		defaultProperties = Collections.unmodifiableMap(map);
	}

	@Override
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
		super.setDataSource(dataSource);
	}

	public void setExcludeFilter(String excludeFilter) {
		this.excludeFilter = excludeFilter;
	}

	@Override
	public void setAnnotatedClasses(Class<?>... annotatedClasses) {
		this.annotatedClasses = annotatedClasses;
	}

	@Override
	public Map<String, String> getDefaultProperties() {
		return defaultProperties;
	}

	protected void fillHibernateProperties() {
		Properties properties = new Properties();
		properties.putAll(defaultProperties);

		for (Field f : AvailableSettings.class.getDeclaredFields()) {
			try {
				String key = (String) f.get(null);
				String value = environment.getProperty(key);
				if (value != null)
					properties.put(key, value);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		DatabaseProduct databaseProduct = null;
		try (Connection conn = dataSource.getConnection()) {
			DatabaseMetaData dbmd = conn.getMetaData();
			databaseProduct = DatabaseProduct.parse(dbmd.getDatabaseProductName());
		} catch (SQLException e) {
			log.error(e.getMessage(), e);
		}

		String value = properties.getProperty(AvailableSettings.BATCH_VERSIONED_DATA);
		boolean supportsExecuteBatch = databaseProduct != DatabaseProduct.ORACLE;
		if (StringUtils.isBlank(value)) {
			properties.put(AvailableSettings.BATCH_VERSIONED_DATA, String.valueOf(supportsExecuteBatch));
		} else if ("true".equals(value) && !supportsExecuteBatch) {
			properties.put(AvailableSettings.BATCH_VERSIONED_DATA, String.valueOf(false));
			log.warn("Override {} to false because this driver returns incorrect row counts from executeBatch()",
					AvailableSettings.BATCH_VERSIONED_DATA);
		}

		value = properties.getProperty(AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS);
		if (StringUtils.isBlank(value))
			properties.put(AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS, databaseProduct != DatabaseProduct.MYSQL);

		if (multiTenantConnectionProvider != null)
			properties.put(AvailableSettings.MULTI_TENANT, MultiTenancyStrategy.SCHEMA);

		// version 5.2 introduce ALLOW_UPDATE_OUTSIDE_TRANSACTION
		// used for RecordAspect.afterCommit()
		properties.put(AvailableSettings.ALLOW_UPDATE_OUTSIDE_TRANSACTION, true);

		properties.put(AvailableSettings.JPA_VALIDATION_FACTORY, validatorFactory);

		setHibernateProperties(properties);
	}

	@Override
	public void afterPropertiesSet() throws IOException {
		Map<String, Class<?>> added = new HashMap<>();
		List<Class<?>> classes = new ArrayList<>();
		if (annotatedClasses != null) {
			for (Class<?> c : annotatedClasses)
				if (!added.containsKey(c.getSimpleName()) || !c.isAssignableFrom(added.get(c.getSimpleName()))) {
					classes.add(c);
					added.put(c.getSimpleName(), c);
				}
		} else {
			Collection<Class<?>> scaned = ClassScanner.scanAnnotated(ClassScanner.getAppPackages(), Entity.class);
			for (Class<?> c : scaned)
				if (!added.containsKey(c.getSimpleName()) || !c.isAssignableFrom(added.get(c.getSimpleName()))) {
					classes.add(c);
					added.put(c.getSimpleName(), c);
				}
		}
		if (StringUtils.isNotBlank(excludeFilter)) {
			Collection<Class<?>> temp = classes;
			classes = new ArrayList<>();
			String[] arr = excludeFilter.split("\\s*,\\s*");
			for (Class<?> clz : temp) {
				boolean exclude = false;
				for (String s : arr) {
					if (org.ironrhino.core.util.StringUtils.matchesWildcard(clz.getName(), s)) {
						exclude = true;
						break;
					}
				}
				if (!exclude)
					classes.add(clz);
			}
		}
		classes.sort(Comparator.comparing(Class::getName));
		annotatedClasses = classes.toArray(new Class<?>[classes.size()]);
		log.info("annotatedClasses: ");
		for (Class<?> clz : annotatedClasses)
			log.info(clz.getName());
		super.setAnnotatedClasses(annotatedClasses);
		if (implicitNamingStrategy != null)
			setImplicitNamingStrategy(implicitNamingStrategy);
		if (physicalNamingStrategy != null)
			setPhysicalNamingStrategy(physicalNamingStrategy);
		if (multiTenantConnectionProvider != null) {
			setMultiTenantConnectionProvider(multiTenantConnectionProvider);
			if (currentTenantIdentifierResolver != null) {
				setCurrentTenantIdentifierResolver(currentTenantIdentifierResolver);
			}
		}
		if (entityInterceptor != null)
			setEntityInterceptor(entityInterceptor);
		fillHibernateProperties();
		super.afterPropertiesSet();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected SessionFactory buildSessionFactory(LocalSessionFactoryBuilder sfb) {
		if (standardServiceInitiators != null)
			for (StandardServiceInitiator<?> s : standardServiceInitiators)
				sfb.getStandardServiceRegistryBuilder().addInitiator(s);
		Collection<Class<?>> converters = ClassScanner.scanAssignable(ClassScanner.getAppPackages(),
				AttributeConverter.class);
		log.info("annotatedConverters: ");
		for (Class<?> clz : converters) {
			if (AnnotationUtils.getAnnotation(clz, Component.class) != null)
				continue;
			Converter c = clz.getAnnotation(Converter.class);
			if (c != null && c.autoApply()) {
				sfb.addAttributeConverter((Class<AttributeConverter<?, ?>>) clz);
				log.info(clz.getName());
			}
		}
		if (attributeConverters != null) {
			for (AttributeConverter<?, ?> ac : attributeConverters) {
				sfb.addAttributeConverter(ac);
				log.info(ac.getClass().getName());
			}
		}
		sfb.registerTypeOverride(YearMonthType.INSTANCE);
		SessionFactory sessionFactory = super.buildSessionFactory(sfb);
		EventListenerRegistry registry = ((SessionFactoryImplementor) sessionFactory).getServiceRegistry()
				.getService(EventListenerRegistry.class);
		registry.setListeners(EventType.SAVE, new SaveCallbackEventListener());
		registry.setListeners(EventType.PERSIST, new PersistCallbackEventListener());
		registry.setListeners(EventType.MERGE, new MergeCallbackEventListener());
		registry.setListeners(EventType.SAVE_UPDATE, new SaveOrUpdateCallbackEventListener());
		registry.setListeners(EventType.FLUSH_ENTITY, new FlushEntityCallbackEventListener());
		registry.setListeners(EventType.DELETE, new DeleteCallbackEventListener());
		registry.appendListeners(EventType.POST_INSERT, new PostInsertCallbackEventListener());
		registry.appendListeners(EventType.POST_UPDATE, new PostUpdateCallbackEventListener());
		registry.appendListeners(EventType.POST_DELETE, new PostDeleteCallbackEventListener());
		registry.appendListeners(EventType.POST_LOAD, new PostLoadCallbackEventListener());
		registry.appendListeners(EventType.PRE_INSERT, preInsertEventListeners);
		registry.appendListeners(EventType.PRE_UPDATE, preUpdateEventListeners);
		registry.appendListeners(EventType.PRE_DELETE, preDeleteEventListeners);
		registry.appendListeners(EventType.POST_INSERT, postInsertEventListeners);
		registry.appendListeners(EventType.POST_UPDATE, postUpdateEventListeners);
		registry.appendListeners(EventType.POST_DELETE, postDeleteEventListeners);
		registry.appendListeners(EventType.POST_LOAD, postLoadEventListeners);
		return sessionFactory;

	}

}
