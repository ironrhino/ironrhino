package org.ironrhino.core.remoting.impl;

import static org.ironrhino.core.metadata.Profiles.DEFAULT;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
	public void register(String serviceName) {
		List<String> hosts = services.putIfAbsent(serviceName, new CopyOnWriteArrayList<>());
		if (hosts == null)
			hosts = services.get(serviceName);
		hosts.add(serviceName);
	}

	@Override
	public void unregister(String serviceName) {
		List<String> hosts = services.get(serviceName);
		if (hosts != null)
			hosts.remove(serviceName);
	}

	@Override
	protected void onReady() {

	}

	@Override
	protected void lookup(String serviceName) {

	}

	@Override
	public Map<String, Collection<String>> getExportedHostsForService(String service) {
		return (exportedServices.containsKey(service))
				? Collections.singletonMap(getLocalHost(), Collections.emptyList())
				: Collections.emptyMap();
	}

	@Override
	public Map<String, String> getImportedHostsForService(String service) {
		return Collections.emptyMap();
	}

	@Override
	public Map<String, String> getImportedServices(String host) {
		return Collections.emptyMap();
	}

	@Override
	public Collection<String> getAllAppNames() {
		return Collections.singleton(AppInfo.getAppName());
	}

	@Override
	public Map<String, String> getExportedServices(String appName) {
		if (AppInfo.getAppName().equals(appName))
			return exportedServiceDescriptions;
		else
			return Collections.emptyMap();
	}
}
