package org.ironrhino.core.remoting;

import java.util.Collection;
import java.util.Map;

public interface ServiceRegistry {

	int DEFAULT_HTTP_PORT = 8080;

	int DEFAULT_HTTPS_PORT = 8443;

	public String getLocalHost();

	public Map<String, Object> getExportedServices();

	public String discover(String serviceName);

	public void evict(String host);

	public Collection<String> getAllAppNames();

	public Map<String, String> getExportedServices(String appName);

	public Map<String, String> getImportedServices(String host);

	public Collection<String> getExportedHostsForService(String service);

	public Collection<String> getImportedHostsForService(String service);

}