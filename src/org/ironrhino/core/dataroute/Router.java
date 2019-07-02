package org.ironrhino.core.dataroute;

import java.util.List;

@FunctionalInterface
public interface Router {

	int route(List<String> nodes, Object routingKey);

}
