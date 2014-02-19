package org.ironrhino.core.websocket;

import javax.websocket.server.ServerEndpointConfig;

import org.ironrhino.core.util.ApplicationContextUtils;

public class SpringServerEndpointConfigurator extends
		ServerEndpointConfig.Configurator {

	@Override
	public <T> T getEndpointInstance(Class<T> endpointClass)
			throws InstantiationException {
		T instance = null;
		instance = ApplicationContextUtils.getBean(endpointClass);
		if (instance == null)
			return super.getEndpointInstance(endpointClass);
		else
			return instance;
	}
}