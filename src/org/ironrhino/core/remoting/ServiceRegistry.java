package org.ironrhino.core.remoting;

import java.util.Collection;
import java.util.Map;

public interface ServiceRegistry {

	int DEFAULT_HTTP_PORT = 8080;

	String getLocalHost();

	// key: service name, value: service object
	Map<String, Object> getExportedServices();

	default void register(String serviceName, Object serviceObject) {
		register(serviceName, null, serviceObject);
	}

	void register(String serviceName, String path, Object serviceObject);

	default void unregister(String serviceName) {
		unregister(serviceName, null);
	}

	void unregister(String serviceName, String path);

	String discover(String serviceName);

	void evict(String host);

	Collection<String> getAllAppNames();

	// key: service name, value: service description
	Map<String, String> getExportedServicesByAppName(String appName);

	Collection<String> getExportedHostsByService(String serviceName);

}