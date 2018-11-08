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
		List<String> hosts = services.putIfAbsent(serviceName, new CopyOnWriteArrayList<>());
		if (hosts == null)
			hosts = services.get(serviceName);
		hosts.add(host);
	}

	@Override
	protected void doUnregister(String serviceName, String host) {
		List<String> hosts = services.get(serviceName);
		if (hosts != null)
			hosts.remove(host);
	}

	@Override
	protected void lookup(String serviceName) {

	}

	@Override
	protected void writeDiscoveredServices() {

	}

	@Override
	protected void writeExportServiceDescriptions() {

	}

	@Override
	public Map<String, Collection<String>> getExportedHostsByService(String service) {
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
			return new TreeMap<>(exportedServiceDescriptions);
		else
			return Collections.emptyMap();
	}
}
