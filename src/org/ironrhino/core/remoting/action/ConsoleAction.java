package org.ironrhino.core.remoting.action;

import java.util.Collection;

import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.JsonConfig;
import org.ironrhino.core.remoting.ServiceRegistry;
import org.ironrhino.core.security.role.UserRole;
import org.ironrhino.core.struts.BaseAction;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.Getter;

@AutoConfig
@Authorize(ifAnyGranted = UserRole.ROLE_ADMINISTRATOR)
public class ConsoleAction extends BaseAction {

	private static final long serialVersionUID = 8180265410790553918L;

	@Autowired
	@Getter
	private ServiceRegistry serviceRegistry;

	@Getter
	private Collection<String> hosts;

	@Override
	public String execute() {
		return SUCCESS;
	}

	@JsonConfig(root = "hosts")
	public String hosts() {
		hosts = serviceRegistry.getExportedHostsByService(getUid());
		return JSON;
	}

}
