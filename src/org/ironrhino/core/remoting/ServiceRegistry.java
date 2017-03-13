package org.ironrhino.core.remoting;

import java.util.Collection;
import java.util.Map;

public interface ServiceRegistry {

	int DEFAULT_HTTP_PORT = 8080;

	int DEFAULT_HTTPS_PORT = 8443;

	public String getLocalHost();

	public Map<String, Object> getExportServices();

	public String discover(String serviceName);

	public void evict(String host);

	public Collection<String> getAllServices();

	public Collection<String> getHostsForService(String service);

	public Map<String, String> getDiscoveredServices(String host);

}