package org.ironrhino.core.websocket;

import javax.websocket.server.ServerEndpointConfig;

import org.ironrhino.core.util.ApplicationContextUtils;

/**
 * 基于Spring框架的服务节点配置器.
 */
public class SpringServerEndpointConfigurator extends
		ServerEndpointConfig.Configurator {

  /**
   * 获取服务节点实例
   */
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