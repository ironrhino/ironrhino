package org.ironrhino.core.remoting;

import java.util.Collection;
import java.util.Map;

public interface ServiceRegistry {

	int DEFAULT_HTTP_PORT = 8080;

	public String getLocalHost();

	// key: service name, value: service object
	public Map<String, Object> getExportedServices();

	public void register(String serviceName, Object serviceObject);

	public void unregister(String serviceName);

	public String discover(String serviceName, boolean polling);

	public void evict(String host);

	public Collection<String> getAllAppNames();

	// key: service name, value: service description
	public Map<String, String> getExportedServicesByAppName(String appName);

	// key: service name, value: service provider host
	public Map<String, String> getImportedServicesByHost(String host);

	// key: service provider host, value: service consumer hosts
	public Map<String, Collection<String>> getExportedHostsByService(String service);

	// key: service consumer host, value: service provider host
	public Map<String, String> getImportedHostsByService(String service);

}