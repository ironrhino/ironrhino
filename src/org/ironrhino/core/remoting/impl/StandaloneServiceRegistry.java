package org.ironrhino.core.remoting.impl;

import static org.ironrhino.core.metadata.Profiles.DEFAULT;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.ironrhino.core.spring.configuration.ServiceImplementationConditional;
import org.springframework.stereotype.Component;

@Component("serviceRegistry")
@ServiceImplementationConditional(profiles = DEFAULT)
public class StandaloneServiceRegistry extends AbstractServiceRegistry {

	@Override
	public Collection<String> getAllServices() {
		return exportServices.keySet();
	}

	@Override
	public Collection<String> getHostsForService(String service) {
		return Collections.singleton(host);
	}

	@Override
	public Map<String, String> getDiscoveredServices(String host) {
		return Collections.emptyMap();
	}
}
