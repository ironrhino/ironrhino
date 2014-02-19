package org.ironrhino.core.hibernate;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.persistence.Entity;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.cfg.AvailableSettings;
import org.ironrhino.core.hibernate.dialect.MyDialectResolver;
import org.ironrhino.core.util.ClassScaner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionFactoryBean extends
		org.springframework.orm.hibernate4.LocalSessionFactoryBean {

	private Logger logger = LoggerFactory.getLogger(getClass());

	private Class<?>[] annotatedClasses;

	private String excludeFilter;

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
		if (StringUtils.isBlank(properties
				.getProperty(AvailableSettings.DIALECT_RESOLVERS)))
			properties.put(AvailableSettings.DIALECT_RESOLVERS,
					MyDialectResolver.class.getName());
		Set<Class<?>> classes = ClassScaner.scanAnnotated(
				ClassScaner.getAppPackages(), Entity.class);
		if (annotatedClasses != null)
			classes.addAll(Arrays.asList(annotatedClasses));
		if (StringUtils.isNotBlank(excludeFilter)) {
			Set<Class<?>> temp = classes;
			classes = new HashSet<Class<?>>();
			String[] arr = excludeFilter.split("\\s*,\\s*");
			for (Class<?> clz : temp) {
				boolean exclude = false;
				for (String s : arr) {
					if (org.ironrhino.core.util.StringUtils.matchesWildcard(
							clz.getName(), s)) {
						exclude = true;
						break;
					}
				}
				if (!exclude)
					classes.add(clz);
			}
		}
		annotatedClasses = classes.toArray(new Class<?>[0]);
		logger.info("annotatedClasses: ");
		for (Class<?> clz : annotatedClasses)
			logger.info(clz.getName());
		super.setAnnotatedClasses(annotatedClasses);
		super.afterPropertiesSet();
	}
}
