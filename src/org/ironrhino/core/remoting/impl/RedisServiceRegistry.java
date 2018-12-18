package org.ironrhino.core.remoting.impl;

import static org.ironrhino.core.metadata.Profiles.CLOUD;
import static org.ironrhino.core.metadata.Profiles.DUAL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;

import javax.annotation.PreDestroy;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.event.EventPublisher;
import org.ironrhino.core.event.InstanceLifecycleEvent;
import org.ironrhino.core.event.InstanceShutdownEvent;
import org.ironrhino.core.metadata.Scope;
import org.ironrhino.core.remoting.ExportServicesEvent;
import org.ironrhino.core.spring.configuration.PriorityQualifier;
import org.ironrhino.core.spring.configuration.ServiceImplementationConditional;
import org.ironrhino.core.util.AppInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
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
	private StringRedisTemplate remotingStringRedisTemplate;

	@Autowired
	private EventPublisher eventPublisher;

	@Autowired(required = false)
	private ExecutorService executorService;

	private Map<String, String> servicePaths = new ConcurrentHashMap<>();

	@Override
	protected void onReady() {
		Set<String> services = getExportedServices().keySet();
		if (!services.isEmpty()) {
			ExportServicesEvent event = new ExportServicesEvent(new ArrayList<>(services), servicePaths);
			eventPublisher.publish(event, Scope.GLOBAL);
		}
		super.onReady();
	}

	@Override
	public void register(String serviceName, String path, Object serviceObject) {
		super.register(serviceName, path, serviceObject);
		if (StringUtils.isNotBlank(path))
			servicePaths.put(serviceName, path);
	}

	@Override
	public void unregister(String serviceName, String path) {
		super.unregister(serviceName, path);
		if (StringUtils.isNotBlank(path))
			servicePaths.remove(serviceName);
	}

	@Override
	protected void lookup(String serviceName) {
		List<String> list = remotingStringRedisTemplate.opsForList().range(NAMESPACE_SERVICES + serviceName, 0, -1);
		if (list != null && list.size() > 0)
			getImportedServiceCandidates().put(serviceName, new CopyOnWriteArrayList<>(list));
	}

	@Override
	protected void doRegister(String serviceName, String host) {
		remotingStringRedisTemplate.opsForList().remove(NAMESPACE_SERVICES + serviceName, 0, host);
		remotingStringRedisTemplate.opsForList().rightPush(NAMESPACE_SERVICES + serviceName, host);
	}

	@Override
	protected void doUnregister(String serviceName, String host) {
		remotingStringRedisTemplate.opsForList().remove(NAMESPACE_SERVICES + serviceName, 0, host);
	}

	@Override
	protected void writeDiscoveredServices() {
		if (importedServices.size() == 0)
			return;
		Runnable task = () -> remotingStringRedisTemplate.opsForHash().putAll(NAMESPACE_HOSTS + getLocalHost(),
				importedServices);
		if (executorService != null)
			executorService.execute(task);
		else
			task.run();
	}

	@Override
	protected void writeExportServiceDescriptions() {
		if (exportedServiceDescriptions.size() == 0)
			return;
		Runnable task = () -> remotingStringRedisTemplate.opsForHash().putAll(NAMESPACE_APPS + AppInfo.getAppName(),
				exportedServiceDescriptions);
		if (executorService != null)
			executorService.execute(task);
		else
			task.run();
	}

	@Override
	protected Map<String, Collection<String>> doGetExportedHostsByService(String service) {
		Map<String, Collection<String>> result = new TreeMap<>();
		Map<String, String> map = getImportedHostsByService(service);
		List<String> hosts = remotingStringRedisTemplate.opsForList().range(NAMESPACE_SERVICES + service, 0, -1);
		if (hosts == null)
			return Collections.emptyMap();
		for (String host : hosts) {
			List<String> consumers = new ArrayList<>();
			for (Map.Entry<String, String> entry : map.entrySet())
				if (entry.getValue().equals(host))
					consumers.add(entry.getKey());
			consumers.sort(null);
			result.put(host, consumers);
		}
		return result;
	}

	@Override
	public Map<String, String> getImportedHostsByService(String service) {
		Map<String, String> result = new TreeMap<>();
		Set<String> hosts = remotingStringRedisTemplate.keys(NAMESPACE_HOSTS + "*");
		if (hosts == null)
			return Collections.emptyMap();
		for (String key : hosts) {
			Map<Object, Object> map = remotingStringRedisTemplate.opsForHash().entries(key);
			if (map.containsKey(service))
				result.put(key.substring(NAMESPACE_HOSTS.length()), (String) map.get(service));
		}
		return result;
	}

	@Override
	public Map<String, String> getImportedServicesByHost(String host) {
		if (host.equals(getLocalHost()))
			return importedServices;
		Map<Object, Object> map = remotingStringRedisTemplate.opsForHash().entries(NAMESPACE_HOSTS + host);
		Map<String, String> services = new TreeMap<>();
		for (Map.Entry<Object, Object> entry : map.entrySet())
			services.put((String) entry.getKey(), (String) entry.getValue());
		return services;
	}

	@Override
	public Collection<String> getAllAppNames() {
		Set<String> keys = remotingStringRedisTemplate.keys(NAMESPACE_APPS + "*");
		if (keys == null)
			return Collections.emptyList();
		List<String> appNames = new ArrayList<>(keys.size());
		for (String s : keys)
			appNames.add(s.substring(NAMESPACE_APPS.length()));
		appNames.sort(null);
		return appNames;
	}

	@Override
	public Map<String, String> getExportedServicesByAppName(String appName) {
		if (AppInfo.getAppName().equals(appName))
			return new TreeMap<>(exportedServiceDescriptions);
		Map<Object, Object> map = remotingStringRedisTemplate.opsForHash().entries(NAMESPACE_APPS + appName);
		Map<String, String> services = new TreeMap<>();
		for (Map.Entry<Object, Object> entry : map.entrySet())
			services.put((String) entry.getKey(), (String) entry.getValue());
		return services;
	}

	@PreDestroy
	@Override
	public void destroy() {
		super.destroy();
		remotingStringRedisTemplate.delete(NAMESPACE_HOSTS + getLocalHost());
	}

	@EventListener(condition = "!#event.local")
	public void onApplicationEvent(InstanceLifecycleEvent event) {
		String instanceId = event.getInstanceId();
		String appName = instanceId.substring(0, instanceId.lastIndexOf('@'));
		appName = appName.substring(0, appName.lastIndexOf('-'));
		String host = appName + instanceId.substring(instanceId.lastIndexOf('@'));
		if (event instanceof InstanceShutdownEvent) {
			evict(host);
		} else if (event instanceof ExportServicesEvent) {
			ExportServicesEvent ev = (ExportServicesEvent) event;
			for (String serviceName : ev.getExportServices()) {
				String path = ev.getServicePaths().get(serviceName);
				String ho = path != null ? host + path : host;
				List<String> hosts = getImportedServiceCandidates().get(serviceName);
				if (hosts != null && !hosts.contains(ho))
					hosts.add(ho);
			}
		}
	}

}
