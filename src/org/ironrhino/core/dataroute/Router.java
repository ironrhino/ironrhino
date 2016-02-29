package org.ironrhino.core.dataroute;

import java.util.List;

@FunctionalInterface
public interface Router {

	public String route(List<String> nodes, String routingKey);

}
