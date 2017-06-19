package org.ironrhino.core.remoting.impl;

import static org.ironrhino.core.metadata.Profiles.CLOUD;
import static org.ironrhino.core.metadata.Profiles.DUAL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;

import javax.annotation.PreDestroy;

import org.ironrhino.core.event.EventPublisher;
import org.ironrhino.core.metadata.Scope;
import org.ironrhino.core.remoting.ExportServicesEvent;
import org.ironrhino.core.spring.configuration.PriorityQualifier;
import org.ironrhino.core.spring.configuration.ServiceImplementationConditional;
import org.ironrhino.core.util.AppInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component("serviceRegistry")
@ServiceImplementationConditional(profiles = { DUAL, CLOUD })
public class RedisServiceRegistry extends AbstractServiceRegistry {

	private static final String NAMESPACE = "remoting:";

	private static final String NAMESPACE_SERVICES = NAMESPACE + "services:";

	private static final String NAMESPACE_HOSTS = NAMESPACE + "hosts:";

	private static final String NAMESPACE_APPS = NAMESPACE + "apps:";

	@Autowired
	@Qualifier("stringRedisTemplate")
	@PriorityQualifier({ "remotingStringRedisTemplate", "globalStringRedisTemplate" })
	private RedisTemplate<String, String> stringRedisTemplate;

	@Autowired
	private EventPublisher eventPublisher;

	@Autowired(required = false)
	private ExecutorService executorService;

	private Map<String, String> discoveredServices = new HashMap<>();

	private boolean ready;

	@Override
	protected void onReady() {
		Set<String> services = getExportedServices().keySet();
		if (!services.isEmpty()) {
			ExportServicesEvent event = new ExportServicesEvent(new ArrayList<>(services));
			eventPublisher.publish(event, Scope.GLOBAL);
		}
		writeDiscoveredServices();
		writeExportServiceDescriptions();
		ready = true;
	}

	@Override
	public String discover(String serviceName, boolean poll) {
		List<String> hosts = importedServiceCandidates.get(serviceName);
		if (hosts == null || hosts.size() == 0)
			lookup(serviceName);
		return super.discover(serviceName, poll);
	}

	@Override
	protected void lookup(String serviceName) {
		List<String> list = stringRedisTemplate.opsForList().range(NAMESPACE_SERVICES + serviceName, 0, -1);
		if (list != null && list.size() > 0)
			importedServiceCandidates.put(serviceName, new CopyOnWriteArrayList<>(list));
	}

	@Override
	protected void register(String serviceName) {
		String host = getLocalHost();
		stringRedisTemplate.opsForList().remove(NAMESPACE_SERVICES + serviceName, 0, host);
		stringRedisTemplate.opsForList().rightPush(NAMESPACE_SERVICES + serviceName, host);
	}

	@Override
	protected void unregister(String serviceName) {
		String host = getLocalHost();
		stringRedisTemplate.opsForList().remove(NAMESPACE_SERVICES + serviceName, 0, host);
	}

	@Override
	protected void onDiscover(String serviceName, String host) {
		super.onDiscover(serviceName, host);
		discoveredServices.put(serviceName, host);
		if (ready)
			writeDiscoveredServices();
	}

	protected void writeDiscoveredServices() {
		if (discoveredServices.size() == 0)
			return;
		Runnable task = () -> stringRedisTemplate.opsForHash().putAll(NAMESPACE_HOSTS + getLocalHost(),
				discoveredServices);
		if (executorService != null)
			executorService.execute(task);
		else
			task.run();
	}

	protected void writeExportServiceDescriptions() {
		if (exportedServiceDescriptions.size() == 0)
			return;
		Runnable task = () -> stringRedisTemplate.opsForHash().putAll(NAMESPACE_APPS + AppInfo.getAppName(),
				exportedServiceDescriptions);
		if (executorService != null)
			executorService.execute(task);
		else
			task.run();
	}

	@Override
	public Map<String, Collection<String>> getExportedHostsForService(String service) {
		Map<String, Collection<String>> result = new TreeMap<>();
		Map<String, String> map = getImportedHostsForService(service);
		for (String host : stringRedisTemplate.opsForList().range(NAMESPACE_SERVICES + service, 0, -1)) {
			List<String> consumers = new ArrayList<>();
			for (Map.Entry<String, String> entry : map.entrySet())
				if (entry.getValue().equals(host))
					consumers.add(entry.getKey());
			Collections.sort(consumers);
			result.put(host, consumers);
		}
		return result;
	}

	@Override
	public Map<String, String> getImportedHostsForService(String service) {
		Map<String, String> result = new TreeMap<>();
		for (String key : stringRedisTemplate.keys(NAMESPACE_HOSTS + "*")) {
			Map<Object, Object> map = stringRedisTemplate.opsForHash().entries(key);
			if (map.containsKey(service))
				result.put(key.substring(NAMESPACE_HOSTS.length()), (String) map.get(service));
		}
		return result;
	}

	@Override
	public Map<String, String> getImportedServices(String host) {
		Map<Object, Object> map = stringRedisTemplate.opsForHash().entries(NAMESPACE_HOSTS + host);
		Map<String, String> services = new TreeMap<>();
		for (Map.Entry<Object, Object> entry : map.entrySet())
			services.put((String) entry.getKey(), (String) entry.getValue());
		return services;
	}

	@Override
	public Collection<String> getAllAppNames() {
		Set<String> keys = stringRedisTemplate.keys(NAMESPACE_APPS + "*");
		List<String> appNames = new ArrayList<>(keys.size());
		for (String s : keys)
			appNames.add(s.substring(NAMESPACE_APPS.length()));
		Collections.sort(appNames);
		return appNames;
	}

	@Override
	public Map<String, String> getExportedServices(String appName) {
		if (AppInfo.getAppName().equals(appName))
			return exportedServiceDescriptions;
		Map<Object, Object> map = stringRedisTemplate.opsForHash().entries(NAMESPACE_APPS + appName);
		Map<String, String> services = new TreeMap<>();
		for (Map.Entry<Object, Object> entry : map.entrySet())
			services.put((String) entry.getKey(), (String) entry.getValue());
		return services;
	}

	@PreDestroy
	@Override
	public void destroy() {
		super.destroy();
		stringRedisTemplate.delete(NAMESPACE_HOSTS + getLocalHost());
	}

}
