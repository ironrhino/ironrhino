package org.ironrhino.core.remoting.impl;

import static org.ironrhino.core.metadata.Profiles.DEFAULT;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.ironrhino.core.spring.configuration.ResourcePresentConditional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component("serviceRegistry")
@Profile(DEFAULT)
@ResourcePresentConditional(value = "resources/spring/applicationContext-remoting.xml", negated = true)
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
