package org.ironrhino.common.action;

import java.io.Writer;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.struts2.ServletActionContext;
import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.security.role.UserRole;
import org.ironrhino.core.spring.ApplicationContextInspector;
import org.ironrhino.core.struts.BaseAction;
import org.springframework.beans.factory.annotation.Autowired;

@AutoConfig
@Authorize(ifAnyGranted = UserRole.ROLE_ADMINISTRATOR)
public class PropertiesAction extends BaseAction {

	private static final long serialVersionUID = 7199381226377678499L;

	@Autowired
	private ApplicationContextInspector applicationContextInspector;

	@Override
	public String execute() throws Exception {
		HttpServletResponse response = ServletActionContext.getResponse();
		response.setContentType("text/plain");
		Writer writer = response.getWriter();
		Map<String, String> defaultProperties = applicationContextInspector.getDefaultProperties();
		Map<String, String> overridedProperties = applicationContextInspector.getOverridedProperties();
		for (Map.Entry<String, String> entry : defaultProperties.entrySet()) {
			String overridedValue = overridedProperties.get(entry.getKey());
			if (overridedValue != null && !overridedValue.equals(entry.getValue()))
				writer.write("#" + entry.getKey() + '=' + entry.getValue() + "\n");
			writer.write(entry.getKey() + '=' + (overridedValue != null ? overridedValue : entry.getValue()) + "\n");
		}
		writer.write("\n");
		for (Map.Entry<String, String> entry : overridedProperties.entrySet())
			if (!defaultProperties.containsKey(entry.getKey()))
				writer.write(entry.getKey() + '=' + entry.getValue() + "\n");
		return NONE;
	}

	public String overrided() throws Exception {
		HttpServletResponse response = ServletActionContext.getResponse();
		response.setContentType("text/plain");
		Writer writer = response.getWriter();
		Map<String, String> overridedProperties = applicationContextInspector.getOverridedProperties();
		for (Map.Entry<String, String> entry : overridedProperties.entrySet())
			writer.write(entry.getKey() + '=' + entry.getValue() + "\n");
		return NONE;
	}

}
