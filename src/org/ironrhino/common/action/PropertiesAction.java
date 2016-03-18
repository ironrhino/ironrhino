package org.ironrhino.common.action;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletResponse;

import org.apache.struts2.ServletActionContext;
import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.security.role.UserRole;
import org.ironrhino.core.struts.BaseAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.ResourcePropertySource;

@AutoConfig
@Authorize(ifAnyGranted = UserRole.ROLE_ADMINISTRATOR)
public class PropertiesAction extends BaseAction {

	private static final long serialVersionUID = 7199381226377678499L;

	@Autowired
	private ConfigurableEnvironment env;

	@Override
	public String execute() throws IOException {
		HttpServletResponse response = ServletActionContext.getResponse();
		response.setContentType("text/plain");
		Writer writer = response.getWriter();
		Map<String, String> properties = new TreeMap<>();
		for (PropertySource<?> ps : env.getPropertySources()) {
			if (ps instanceof ResourcePropertySource) {
				ResourcePropertySource rps = (ResourcePropertySource) ps;
				for (String s : rps.getPropertyNames())
					properties.put(s, s.endsWith(".password") ? "********" : env.getProperty(s));
			}
		}
		for (Map.Entry<String, String> entry : properties.entrySet())
			writer.write(entry.getKey() + "=" + entry.getValue() + "\n");
		return NONE;
	}

}
