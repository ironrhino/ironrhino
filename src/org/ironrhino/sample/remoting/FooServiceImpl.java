package org.ironrhino.sample.remoting;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.stereotype.Component;

@Component
public class FooServiceImpl implements FactoryBean<FooService> {

	private final FooService service;

	public FooServiceImpl() {
		service = s -> s;

	}

	@Override
	public FooService getObject() throws Exception {
		return service;
	}

	@Override
	public Class<?> getObjectType() {
		return FooService.class;
	}

}
