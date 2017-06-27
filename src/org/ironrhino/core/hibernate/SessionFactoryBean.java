package org.ironrhino.core.hibernate;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import javax.persistence.Entity;
import javax.sql.DataSource;

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
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.FlushEntityEventListener;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostLoadEventListener;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.event.spi.PreDeleteEventListener;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.hibernate.internal.SessionFactoryImpl;
import org.ironrhino.core.hibernate.dialect.MyDialectResolver;
import org.ironrhino.core.jdbc.DatabaseProduct;
import org.ironrhino.core.util.ClassScanner;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.orm.hibernate5.LocalSessionFactoryBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.validation.beanvalidation.LocaleContextMessageInterpolator;

public class SessionFactoryBean extends org.springframework.orm.hibernate5.LocalSessionFactoryBean {

	@Autowired
	private Logger logger;

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
	private List<PreInsertEventListener> preInsertEventListeners;

	@Autowired(required = false)
	private List<PreUpdateEventListener> preUpdateEventListeners;

	@Autowired(required = false)
	private List<PreDeleteEventListener> preDeleteEventListeners;

	@Autowired(required = false)
	private List<PostInsertEventListener> postInsertEventListeners;

	@Autowired(required = false)
	private List<PostUpdateEventListener> postUpdateEventListeners;

	@Autowired(required = false)
	private List<PostDeleteEventListener> postDeleteEventListeners;

	@Autowired(required = false)
	private List<PostLoadEventListener> postLoadEventListeners;

	@Autowired(required = false)
	private List<FlushEntityEventListener> flushEntityEventListeners;

	private Class<?>[] annotatedClasses;

	private String excludeFilter;

	private DataSource dataSource;

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
	public void afterPropertiesSet() throws IOException {
		Properties properties = getHibernateProperties();
		if (StringUtils.isBlank(properties.getProperty(AvailableSettings.DIALECT_RESOLVERS)))
			properties.put(AvailableSettings.DIALECT_RESOLVERS, MyDialectResolver.class.getName());
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
		Collections.sort(classes, (a, b) -> a.getName().compareTo(b.getName()));
		annotatedClasses = classes.toArray(new Class<?>[0]);
		logger.info("annotatedClasses: ");
		for (Class<?> clz : annotatedClasses)
			logger.info(clz.getName());
		super.setAnnotatedClasses(annotatedClasses);
		if (implicitNamingStrategy != null)
			setImplicitNamingStrategy(implicitNamingStrategy);
		if (physicalNamingStrategy != null)
			setPhysicalNamingStrategy(physicalNamingStrategy);
		if (multiTenantConnectionProvider != null) {
			getHibernateProperties().put(AvailableSettings.MULTI_TENANT, MultiTenancyStrategy.SCHEMA);
			setMultiTenantConnectionProvider(multiTenantConnectionProvider);
			if (currentTenantIdentifierResolver != null) {
				setCurrentTenantIdentifierResolver(currentTenantIdentifierResolver);
			}
		}
		if (entityInterceptor != null)
			setEntityInterceptor(entityInterceptor);
		Properties props = getHibernateProperties();
		String value = props.getProperty(AvailableSettings.BATCH_VERSIONED_DATA);
		if ("true".equals(value)) {
			try (Connection conn = dataSource.getConnection()) {
				DatabaseMetaData dbmd = conn.getMetaData();
				DatabaseProduct dp = DatabaseProduct.parse(dbmd.getDatabaseProductName());
				if (dp == DatabaseProduct.ORACLE) {
					props.put(AvailableSettings.BATCH_VERSIONED_DATA, "false");
					logger.warn(
							"Override {} to false because this driver returns incorrect row counts from executeBatch()",
							AvailableSettings.BATCH_VERSIONED_DATA);
				}
			} catch (SQLException e) {
				logger.error(e.getMessage(), e);
			}
		}
		if (ClassUtils.isPresent("javax.validation.Configuration", getClass().getClassLoader())) {
			final javax.validation.Configuration<?> configuration = javax.validation.Validation.byDefaultProvider()
					.configure();
			configuration.messageInterpolator(
					new LocaleContextMessageInterpolator(configuration.getDefaultMessageInterpolator()));
			props.put("javax.persistence.validation.factory", configuration.buildValidatorFactory());
		}
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
		logger.info("annotatedConverters: ");
		for (Class<?> clz : converters) {
			if (AnnotationUtils.getAnnotation(clz, Component.class) != null)
				continue;
			Converter c = clz.getAnnotation(Converter.class);
			if (c != null && c.autoApply()) {
				sfb.addAttributeConverter((Class<AttributeConverter<?, ?>>) clz);
				logger.info(clz.getName());
			}
		}
		if (attributeConverters != null) {
			for (AttributeConverter<?, ?> ac : attributeConverters) {
				sfb.addAttributeConverter(ac);
				logger.info(ac.getClass().getName());
			}
		}
		SessionFactory sessionFactory = sfb.buildSessionFactory();
		SessionFactoryImpl sf = (SessionFactoryImpl) sessionFactory;
		EventListenerRegistry registry = sf.getServiceRegistry().getService(EventListenerRegistry.class);
		if (preInsertEventListeners != null)
			registry.appendListeners(EventType.PRE_INSERT,
					preInsertEventListeners.toArray(new PreInsertEventListener[0]));
		if (preUpdateEventListeners != null)
			registry.appendListeners(EventType.PRE_UPDATE,
					preUpdateEventListeners.toArray(new PreUpdateEventListener[0]));
		if (preDeleteEventListeners != null)
			registry.appendListeners(EventType.PRE_DELETE,
					preDeleteEventListeners.toArray(new PreDeleteEventListener[0]));
		if (postInsertEventListeners != null)
			registry.appendListeners(EventType.POST_INSERT,
					postInsertEventListeners.toArray(new PostInsertEventListener[0]));
		if (postUpdateEventListeners != null)
			registry.appendListeners(EventType.POST_UPDATE,
					postUpdateEventListeners.toArray(new PostUpdateEventListener[0]));
		if (postDeleteEventListeners != null)
			registry.appendListeners(EventType.POST_DELETE,
					postDeleteEventListeners.toArray(new PostDeleteEventListener[0]));
		if (postLoadEventListeners != null)
			registry.appendListeners(EventType.POST_LOAD, postLoadEventListeners.toArray(new PostLoadEventListener[0]));
		if (flushEntityEventListeners != null)
			registry.appendListeners(EventType.FLUSH_ENTITY,
					flushEntityEventListeners.toArray(new FlushEntityEventListener[0]));
		return sessionFactory;

	}

}
