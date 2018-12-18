package org.ironrhino.core.remoting.impl;

import static org.ironrhino.core.metadata.Profiles.CLUSTER;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.PostConstruct;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.ironrhino.core.spring.configuration.ServiceImplementationConditional;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.JsonUtils;
import org.ironrhino.core.zookeeper.WatchedEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("serviceRegistry")
@ServiceImplementationConditional(profiles = CLUSTER)
public class ZooKeeperServiceRegistry extends AbstractServiceRegistry implements WatchedEventListener {

	public static final String DEFAULT_ZOOKEEPER_PATH = "/remoting";

	private CuratorFramework curatorFramework;

	@Value("${serviceRegistry.zooKeeperPath:" + DEFAULT_ZOOKEEPER_PATH + "}")
	private String zooKeeperPath = DEFAULT_ZOOKEEPER_PATH;

	private String servicesParentPath;

	private String hostsParentPath;

	private String appsParentPath;

	@Autowired
	public ZooKeeperServiceRegistry(CuratorFramework curatorFramework) {
		this.curatorFramework = curatorFramework;
	}

	@PostConstruct
	public void afterPropertiesSet() {
		servicesParentPath = zooKeeperPath + "/services";
		hostsParentPath = zooKeeperPath + "/hosts";
		appsParentPath = zooKeeperPath + "/apps";
	}

	@Override
	protected void lookup(String serviceName) {
		String path = new StringBuilder(servicesParentPath).append("/").append(serviceName).toString();
		try {
			List<String> children = curatorFramework.getChildren().watched().forPath(path);
			if (children != null && children.size() > 0) {
				List<String> hosts = new CopyOnWriteArrayList<>();
				for (String host : children)
					hosts.add(unescapeSlash(host));
				getImportedServiceCandidates().put(serviceName, hosts);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	protected void doRegister(String serviceName, String host) {
		String path = new StringBuilder().append(servicesParentPath).append("/").append(serviceName).append("/")
				.append(escapeSlash(host)).toString();
		try {
			curatorFramework.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(path);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	protected void doUnregister(String serviceName, String host) {
		String path = new StringBuilder().append(servicesParentPath).append("/").append(serviceName).append("/")
				.append(escapeSlash(host)).toString();
		try {
			curatorFramework.delete().forPath(path);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	protected void writeDiscoveredServices() {
		if (importedServices.size() == 0)
			return;
		String path = new StringBuilder().append(hostsParentPath).append("/").append(escapeSlash(getLocalHost()))
				.toString();
		byte[] data = JsonUtils.toJson(importedServices).getBytes();
		try {
			curatorFramework.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).inBackground()
					.forPath(path, data);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	protected void writeExportServiceDescriptions() {
		if (exportedServiceDescriptions.size() == 0)
			return;
		String path = new StringBuilder().append(appsParentPath).append("/").append(escapeSlash(AppInfo.getAppName()))
				.toString();
		byte[] data = JsonUtils.toJson(exportedServiceDescriptions).getBytes();
		try {
			curatorFramework.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).inBackground()
					.forPath(path, data);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	protected Map<String, Collection<String>> doGetExportedHostsByService(String service) {
		try {
			Map<String, Collection<String>> result = new TreeMap<>();
			Map<String, String> map = getImportedHostsByService(service);
			List<String> children = curatorFramework.getChildren().watched()
					.forPath(new StringBuilder().append(servicesParentPath).append("/").append(service).toString());
			for (String host : children) {
				host = unescapeSlash(host);
				List<String> consumers = new ArrayList<>();
				for (Map.Entry<String, String> entry : map.entrySet())
					if (entry.getValue().equals(host))
						consumers.add(entry.getKey());
				consumers.sort(null);
				result.put(host, consumers);
			}
			return result;
		} catch (NoNodeException e) {
			return Collections.emptyMap();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Collections.emptyMap();
		}
	}

	@Override
	public Map<String, String> getImportedHostsByService(String service) {
		try {
			Map<String, String> result = new TreeMap<>();
			for (String host : curatorFramework.getChildren().forPath(hostsParentPath)) {
				Map<String, String> importedServices = getImportedServicesByHost(host);
				if (importedServices.containsKey(service))
					result.put(host, importedServices.get(service));
			}
			return result;
		} catch (NoNodeException e) {
			return Collections.emptyMap();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Collections.emptyMap();
		}
	}

	@Override
	public Map<String, String> getImportedServicesByHost(String host) {
		if (host.equals(getLocalHost()))
			return importedServices;
		try {
			String path = new StringBuilder().append(hostsParentPath).append("/").append(escapeSlash(host)).toString();
			byte[] data = curatorFramework.getData().forPath(path);
			String sdata = new String(data);
			Map<String, String> map = JsonUtils.fromJson(sdata, JsonUtils.STRING_MAP_TYPE);
			Map<String, String> services = new TreeMap<>();
			services.putAll(map);
			return services;
		} catch (NoNodeException e) {
			return Collections.emptyMap();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Collections.emptyMap();
		}
	}

	@Override
	public Collection<String> getAllAppNames() {
		try {
			List<String> list = curatorFramework.getChildren().forPath(appsParentPath);
			List<String> services = new ArrayList<>(list.size());
			services.addAll(list);
			services.sort(null);
			return services;
		} catch (NoNodeException e) {
			return Collections.emptyList();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Collections.emptyList();
		}
	}

	@Override
	public Map<String, String> getExportedServicesByAppName(String appName) {
		if (AppInfo.getAppName().equals(appName))
			return new TreeMap<>(exportedServiceDescriptions);
		try {
			String path = new StringBuilder().append(appsParentPath).append("/").append(escapeSlash(appName))
					.toString();
			byte[] data = curatorFramework.getData().forPath(path);
			String sdata = new String(data);
			Map<String, String> map = JsonUtils.fromJson(sdata, JsonUtils.STRING_MAP_TYPE);
			Map<String, String> services = new TreeMap<>();
			services.putAll(map);
			return services;
		} catch (NoNodeException e) {
			logger.error(e.getMessage(), e);
			return Collections.emptyMap();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Collections.emptyMap();
		}
	}

	@Override
	public boolean supports(String path) {
		if (path != null && path.startsWith(servicesParentPath)) {
			String serviceName = path.substring(servicesParentPath.length() + 1);
			return getImportedServiceCandidates().containsKey(serviceName);
		}
		return false;
	}

	@Override
	public void onNodeChildrenChanged(String path, List<String> children) {
		String serviceName = path.substring(servicesParentPath.length() + 1);
		List<String> hosts = new ArrayList<>(children.size());
		for (String host : children)
			hosts.add(unescapeSlash(host));
		getImportedServiceCandidates().put(serviceName, new CopyOnWriteArrayList<>(hosts));
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

	private static String escapeSlash(String host) {
		return host.replaceAll("/", "\\$");
	}

	private static String unescapeSlash(String host) {
		return host.replaceAll("\\$", "/");
	}

}
