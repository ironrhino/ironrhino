package org.ironrhino.core.hibernate;

import java.util.Map;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.id.factory.spi.MutableIdentifierGeneratorFactory;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.ironrhino.core.spring.configuration.BeanPresentConditional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@SuppressWarnings("rawtypes")
@Component
@BeanPresentConditional(type = SessionFactory.class)
public class SpringIdentifierGeneratorFactoryInitiator
		implements StandardServiceInitiator<MutableIdentifierGeneratorFactory> {

	@Autowired
	private SpringIdentifierGeneratorFactory identifierGeneratorFactory;

	@Override
	public Class<MutableIdentifierGeneratorFactory> getServiceInitiated() {
		return MutableIdentifierGeneratorFactory.class;
	}

	@Override
	public MutableIdentifierGeneratorFactory initiateService(Map configurationValues,
			ServiceRegistryImplementor registry) {
		return identifierGeneratorFactory;
	}

}
