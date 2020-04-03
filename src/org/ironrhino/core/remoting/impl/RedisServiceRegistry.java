package org.ironrhino.core.remoting.impl;

import static org.ironrhino.core.metadata.Profiles.CLOUD;
import static org.ironrhino.core.metadata.Profiles.DUAL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;

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
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component("serviceRegistry")
@ServiceImplementationConditional(profiles = { DUAL, CLOUD })
public class RedisServiceRegistry extends AbstractServiceRegistry {

	protected static final String NAMESPACE = "remoting:";

	protected static final String NAMESPACE_SERVICES = NAMESPACE + "services:";

	protected static final String NAMESPACE_APPS = NAMESPACE + "apps:";

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
	protected void writeExportServiceDescriptions() {
		if (getExportedServiceDescriptions().isEmpty())
			return;
		Runnable task = () -> remotingStringRedisTemplate.opsForHash().putAll(NAMESPACE_APPS + AppInfo.getAppName(),
				getExportedServiceDescriptions());
		if (executorService != null)
			executorService.execute(task);
		else
			task.run();
	}

	@Override
	protected Collection<String> doGetExportedHostsByService(String serviceName) {
		return remotingStringRedisTemplate.opsForList().range(NAMESPACE_SERVICES + serviceName, 0, -1);
	}

	@Override
	public Collection<String> getAllAppNames() {
		Set<String> keys = remotingStringRedisTemplate.<Set<String>>execute((RedisConnection conn) -> {
			Set<String> set = new HashSet<>();
			Cursor<byte[]> cursor = conn
					.scan(new ScanOptions.ScanOptionsBuilder().match(NAMESPACE_APPS + "*").count(100).build());
			while (cursor.hasNext())
				set.add((String) remotingStringRedisTemplate.getKeySerializer().deserialize(cursor.next()));
			return set;
		});
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
			return new TreeMap<>(getExportedServiceDescriptions());
		Map<Object, Object> map = remotingStringRedisTemplate.opsForHash().entries(NAMESPACE_APPS + appName);
		Map<String, String> services = new TreeMap<>();
		map.forEach((k, v) -> {
			services.put((String) k, (String) v);
		});
		return services;
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
				if (hosts != null && !hosts.contains(ho)) {
					hosts.add(ho);
				}
			}
		}
	}

}
