package org.ironrhino.core.remoting.impl;

import static org.ironrhino.core.metadata.Profiles.CLUSTER;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.ironrhino.core.event.InstanceLifecycleEvent;
import org.ironrhino.core.event.InstanceShutdownEvent;
import org.ironrhino.core.remoting.ExportServicesEvent;
import org.ironrhino.core.util.JsonUtils;
import org.ironrhino.core.zookeeper.WatchedEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component("serviceRegistry")
@Profile(CLUSTER)
public class ZooKeeperServiceRegistry extends AbstractServiceRegistry implements
		WatchedEventListener {

	public static final String DEFAULT_ZOOKEEPER_PATH = "/remoting";

	@Autowired
	private CuratorFramework curatorFramework;

	@Value("${serviceRegistry.zooKeeperPath:" + DEFAULT_ZOOKEEPER_PATH + "}")
	private String zooKeeperPath = DEFAULT_ZOOKEEPER_PATH;

	private Map<String, String> discoveredServices = new HashMap<String, String>();

	private boolean ready;

	private String servicesParentPath;

	private String hostsParentPath;

	@Override
	public void prepare() {
		servicesParentPath = zooKeeperPath + "/services";
		hostsParentPath = zooKeeperPath + "/hosts";
	}

	@Override
	public void onReady() {
		writeDiscoveredServices();
		ready = true;
	}

	@Override
	protected void lookup(String serviceName) {
		String path = new StringBuilder(servicesParentPath).append("/")
				.append(serviceName).toString();
		try {
			List<String> children = curatorFramework.getChildren().watched()
					.forPath(path);
			if (children != null && children.size() > 0)
				importServices.put(serviceName, children);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	@Override
	public void doRegister(String serviceName, String host) {
		String path = new StringBuilder().append(servicesParentPath)
				.append("/").append(serviceName).append("/").append(host)
				.toString();
		try {
			curatorFramework.create().creatingParentsIfNeeded()
					.withMode(CreateMode.EPHEMERAL).forPath(path);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
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
		String path = new StringBuilder().append(hostsParentPath).append("/")
				.append(host).toString();
		byte[] data = JsonUtils.toJson(discoveredServices).getBytes();
		try {
			curatorFramework.create().creatingParentsIfNeeded()
					.withMode(CreateMode.EPHEMERAL).inBackground()
					.forPath(path, data);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	@Override
	public Collection<String> getAllServices() {
		try {
			List<String> list = curatorFramework.getChildren().forPath(
					servicesParentPath);
			List<String> services = new ArrayList<String>(list.size());
			services.addAll(list);
			Collections.sort(services);
			return services;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return Collections.emptyList();
		}
	}

	@Override
	public Collection<String> getHostsForService(String service) {
		try {
			List<String> list = curatorFramework
					.getChildren()
					.watched()
					.forPath(
							new StringBuilder().append(servicesParentPath)
									.append("/").append(service).toString());
			List<String> hosts = new ArrayList<String>(list.size());
			hosts.addAll(list);
			Collections.sort(hosts);
			return hosts;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return Collections.emptyList();
		}
	}

	@Override
	public Map<String, String> getDiscoveredServices(String host) {
		if (host.indexOf(':') < 0)
			host += ":" + DEFAULT_PORT;
		try {
			String path = new StringBuilder().append(hostsParentPath)
					.append("/").append(host).toString();
			byte[] data = curatorFramework.getData().forPath(path);
			String sdata = new String(data);
			Map<String, String> map = JsonUtils.fromJson(sdata,
					JsonUtils.STRING_MAP_TYPE);
			Map<String, String> services = new TreeMap<String, String>();
			services.putAll(map);
			return services;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return Collections.emptyMap();
		}

	}

	@Override
	public boolean supports(String path) {
		if (path != null && path.startsWith(servicesParentPath)) {
			String serviceName = path
					.substring(servicesParentPath.length() + 1);
			return importServices.containsKey(serviceName);
		}
		return false;
	}

	@Override
	public void onNodeChildrenChanged(String path, List<String> children) {
		String serviceName = path.substring(servicesParentPath.length() + 1);
		importServices.put(serviceName, children);
	}

	@Override
	public void onNodeCreated(String path, byte[] data) {

	}

	@Override
	public void onNodeDeleted(String path) {

	}

	@Override
	public void onNodeDataChanged(String path, byte[] data) {

	}

	@Override
	protected boolean handle(InstanceLifecycleEvent event) {
		return event instanceof ExportServicesEvent
				|| event instanceof InstanceShutdownEvent;
		// zookeeper has onNodeChildrenChanged
	}

}
