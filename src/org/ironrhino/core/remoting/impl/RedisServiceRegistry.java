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
import java.util.concurrent.ExecutorService;

import org.ironrhino.core.event.EventPublisher;
import org.ironrhino.core.metadata.Scope;
import org.ironrhino.core.remoting.ExportServicesEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component("serviceRegistry")
@Profile({ DUAL, CLOUD })
public class RedisServiceRegistry extends AbstractServiceRegistry {

	private static final String NAMESPACE = "remoting:";

	private static final String NAMESPACE_SERVICES = NAMESPACE + "services:";

	private static final String NAMESPACE_HOSTS = NAMESPACE + "hosts:";

	@Autowired
	@Qualifier("stringRedisTemplate")
	private RedisTemplate<String, String> stringRedisTemplate;

	@Autowired
	private EventPublisher eventPublisher;

	@Autowired(required = false)
	private ExecutorService executorService;

	private Map<String, String> discoveredServices = new HashMap<String, String>();

	private boolean ready;

	@Override
	protected void onReady() {
		Set<String> services = getExportServices().keySet();
		if (!services.isEmpty()) {
			ExportServicesEvent event = new ExportServicesEvent(
					new ArrayList<String>(services));
			eventPublisher.publish(event, Scope.GLOBAL);
		}
		writeDiscoveredServices();
		ready = true;
	}

	@Override
	public String discover(String serviceName) {
		List<String> hosts = getImportServices().get(serviceName);
		if (hosts == null || hosts.size() == 0)
			lookup(serviceName);
		return super.discover(serviceName);
	}

	@Override
	protected void lookup(String serviceName) {
		List<String> list = stringRedisTemplate.opsForList().range(
				NAMESPACE_SERVICES + serviceName, 0, -1);
		if (list != null && list.size() > 0)
			importServices.put(serviceName, list);
	}

	@Override
	protected void doRegister(String serviceName, String host) {
		stringRedisTemplate.opsForList().rightPush(
				NAMESPACE_SERVICES + serviceName, host);
	}

	@Override
	protected void doUnregister(String serviceName, String host) {
		stringRedisTemplate.opsForList().remove(
				NAMESPACE_SERVICES + serviceName, 0, host);
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
		Runnable task = new Runnable() {
			@Override
			public void run() {
				stringRedisTemplate.opsForHash().putAll(NAMESPACE_HOSTS + host,
						discoveredServices);
			}
		};
		if (executorService != null)
			executorService.execute(task);
		else
			task.run();
	}

	@Override
	public Collection<String> getAllServices() {
		Set<String> keys = stringRedisTemplate.keys(NAMESPACE_SERVICES + "*");
		List<String> services = new ArrayList<String>(keys.size());
		for (String s : keys)
			services.add(s.substring(NAMESPACE_SERVICES.length()));
		Collections.sort(services);
		return services;
	}

	@Override
	public Collection<String> getHostsForService(String service) {
		List<String> list = stringRedisTemplate.opsForList().range(
				NAMESPACE_SERVICES + service, 0, -1);
		List<String> hosts = new ArrayList<String>(list.size());
		hosts.addAll(list);
		Collections.sort(hosts);
		return hosts;
	}

	@Override
	public Map<String, String> getDiscoveredServices(String host) {
		if (host.indexOf(':') < 0)
			host += ":" + DEFAULT_PORT;
		Map<Object, Object> map = stringRedisTemplate.opsForHash().entries(
				NAMESPACE_HOSTS + host);
		Map<String, String> services = new TreeMap<String, String>();
		for (Map.Entry<Object, Object> entry : map.entrySet())
			services.put((String) entry.getKey(), (String) entry.getValue());
		return services;
	}

}
