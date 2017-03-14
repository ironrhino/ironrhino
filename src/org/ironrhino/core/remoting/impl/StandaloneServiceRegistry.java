package org.ironrhino.core.remoting.impl;

import static org.ironrhino.core.metadata.Profiles.DEFAULT;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.ironrhino.core.spring.configuration.ServiceImplementationConditional;
import org.ironrhino.core.util.AppInfo;
import org.springframework.stereotype.Component;

@Component("serviceRegistry")
@ServiceImplementationConditional(profiles = DEFAULT)
public class StandaloneServiceRegistry extends AbstractServiceRegistry {

	@Override
	protected void register(String serviceName) {

	}

	@Override
	protected void unregister(String serviceName) {

	}

	@Override
	protected void onReady() {

	}

	@Override
	protected void lookup(String serviceName) {

	}

	@Override
	public Collection<String> getExportedHostsForService(String service) {
		return Collections.singleton(getLocalHost());
	}

	@Override
	public Collection<String> getImportedHostsForService(String service) {
		return Collections.emptyList();
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
