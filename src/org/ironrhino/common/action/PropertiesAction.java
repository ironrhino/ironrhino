package org.ironrhino.common.action;

import java.io.Writer;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.struts2.ServletActionContext;
import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.security.role.UserRole;
import org.ironrhino.core.spring.ApplicationContextInspector;
import org.ironrhino.core.spring.ApplicationContextInspector.ApplicationProperty;
import org.ironrhino.core.struts.BaseAction;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.Setter;

@AutoConfig
@Authorize(ifAnyGranted = UserRole.ROLE_ADMINISTRATOR)
public class PropertiesAction extends BaseAction {

	private static final long serialVersionUID = 7199381226377678499L;

	@Autowired
	private ApplicationContextInspector applicationContextInspector;

	@Setter
	private boolean brief;

	@Override
	public String execute() throws Exception {
		HttpServletResponse response = ServletActionContext.getResponse();
		response.setContentType("text/plain");
		Writer writer = response.getWriter();
		Map<String, ApplicationProperty> defaultProperties = applicationContextInspector.getDefaultProperties();
		Map<String, ApplicationProperty> overriddenProperties = applicationContextInspector.getOverriddenProperties();
		for (Map.Entry<String, ApplicationProperty> entry : defaultProperties.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue().getValue();
			ApplicationProperty orverridden = overriddenProperties.get(key);
			if (orverridden != null && !orverridden.getValue().equals(value) && !key.startsWith("app.")) {
				writer.write("#" + key + '=' + value + "\n");
			}
			writer.write(key + '=' + (orverridden != null ? orverridden.getValue() : value) + "\n");
			if (!brief)
				writer.write("#defined in " + entry.getValue().getDefinedSource()
						+ (orverridden != null ? ", overridden in " + orverridden.getOverriddenSource() : "") + "\n\n");
		}
		writer.write("\n");
		writer.write("\n");
		for (Map.Entry<String, ApplicationProperty> entry : overriddenProperties.entrySet())
			if (!defaultProperties.containsKey(entry.getKey())) {
				writer.write(entry.getKey() + '=' + entry.getValue().getValue() + "\n");
				if (!brief)
					writer.write("#defined in " + entry.getValue().getOverriddenSource() + "\n\n");
			}
		return NONE;
	}

	public String overridden() throws Exception {
		HttpServletResponse response = ServletActionContext.getResponse();
		response.setContentType("text/plain");
		Writer writer = response.getWriter();
		Map<String, ApplicationProperty> overriddenProperties = applicationContextInspector.getOverriddenProperties();
		for (Map.Entry<String, ApplicationProperty> entry : overriddenProperties.entrySet()) {
			writer.write(entry.getKey() + '=' + entry.getValue().getValue() + "\n");
			if (!brief)
				writer.write("#defined in " + entry.getValue().getOverriddenSource() + "\n\n");
		}
		return NONE;
	}

}
