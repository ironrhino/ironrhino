package org.ironrhino.core.hibernate;

import java.util.Collections;
import java.util.Map;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.jpa.HibernateMetrics;

@Component
@HibernateEnabled
public class HibernateMeterBinder implements MeterBinder {

	@Autowired
	private Map<String, SessionFactory> sessionFactoryMap;

	@Override
	public void bindTo(MeterRegistry meterRegistry) {
		sessionFactoryMap.forEach((sessionFactoryName, sessionFactory) -> {
			new HibernateMetrics(sessionFactory, sessionFactoryName, Collections.emptyList()).bindTo(meterRegistry);
		});

	}

}