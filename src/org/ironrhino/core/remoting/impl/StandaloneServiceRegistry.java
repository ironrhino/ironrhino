package org.ironrhino.core.remoting.impl;

import static org.ironrhino.core.metadata.Profiles.DEFAULT;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.ironrhino.core.spring.configuration.ServiceImplementationConditional;
import org.ironrhino.core.util.AppInfo;
import org.springframework.stereotype.Component;

@Component("serviceRegistry")
@ServiceImplementationConditional(profiles = DEFAULT)
public class StandaloneServiceRegistry extends AbstractServiceRegistry {

	protected Map<String, List<String>> services = new ConcurrentHashMap<>();

	@Override
	protected void doRegister(String serviceName, String host) {
		services.computeIfAbsent(serviceName, key -> new CopyOnWriteArrayList<>()).add(host);
	}

	@Override
	protected void doUnregister(String serviceName, String host) {
		services.computeIfPresent(serviceName, (key, hosts) -> {
			hosts.remove(host);
			return hosts;
		});
	}

	@Override
	protected void lookup(String serviceName) {
		getImportedServiceCandidates().put(serviceName, services.get(serviceName));
	}

	@Override
	protected void writeDiscoveredServices() {

	}

	@Override
	protected void writeExportServiceDescriptions() {

	}

	@Override
	protected Map<String, Collection<String>> doGetExportedHostsByService(String service) {
		return (getExportedServices().containsKey(service))
				? Collections.singletonMap(getLocalHost(), Collections.emptyList())
				: Collections.emptyMap();
	}

	@Override
	public Map<String, String> getImportedHostsByService(String service) {
		return Collections.emptyMap();
	}

	@Override
	public Map<String, String> getImportedServicesByHost(String host) {
		return Collections.emptyMap();
	}

	@Override
	public Collection<String> getAllAppNames() {
		return Collections.singleton(AppInfo.getAppName());
	}

	@Override
	public Map<String, String> getExportedServicesByAppName(String appName) {
		if (AppInfo.getAppName().equals(appName))
			return new TreeMap<>(getExportedServiceDescriptions());
		else
			return Collections.emptyMap();
	}
}
