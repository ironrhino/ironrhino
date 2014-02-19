package org.ironrhino.core.remoting.action;

import java.util.Collection;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.JsonConfig;
import org.ironrhino.core.remoting.ServiceRegistry;
import org.ironrhino.core.security.role.UserRole;
import org.ironrhino.core.struts.BaseAction;

@AutoConfig
@Authorize(ifAnyGranted = UserRole.ROLE_ADMINISTRATOR)
public class ConsoleAction extends BaseAction {

	private static final long serialVersionUID = 8180265410790553918L;

	@Autowired
	private transient ServiceRegistry serviceRegistry;

	private Collection<String> hosts;

	private Map<String, String> discoveredServices;

	public Collection<String> getHosts() {
		return hosts;
	}

	public Map<String, String> getDiscoveredServices() {
		return discoveredServices;
	}

	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	@Override
	public String execute() {
		return SUCCESS;
	}

	@JsonConfig(root = "hosts")
	public String hosts() {
		hosts = serviceRegistry.getHostsForService(getUid());
		return JSON;
	}

	@JsonConfig(root = "discoveredServices")
	public String services() {
		discoveredServices = serviceRegistry.getDiscoveredServices(getUid());
		return JSON;
	}

}
