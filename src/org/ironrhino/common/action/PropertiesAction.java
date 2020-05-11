package org.ironrhino.common.action;

import java.io.Writer;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.struts2.ServletActionContext;
import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.security.role.UserRole;
import org.ironrhino.core.spring.ApplicationContextInspector;
import org.ironrhino.core.spring.ApplicationContextInspector.PropertyDescriptor;
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
	private boolean detail;

	@Override
	public String execute() throws Exception {
		HttpServletResponse response = ServletActionContext.getResponse();
		response.setContentType("text/plain");
		Writer writer = response.getWriter();
		Map<String, PropertyDescriptor> defaultProperties = applicationContextInspector.getDefaultProperties();
		Map<String, PropertyDescriptor> overridedProperties = applicationContextInspector.getOverridedProperties();
		for (Map.Entry<String, PropertyDescriptor> entry : defaultProperties.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue().getValue();
			PropertyDescriptor orverrided = overridedProperties.get(key);
			if (orverrided != null && !orverrided.getValue().equals(value) && !key.startsWith("app.")) {
				writer.write("#" + key + '=' + value + "\n");
			}
			writer.write(key + '=' + (orverrided != null ? orverrided.getValue() : value) + "\n");
			if (detail)
				writer.write("#defined in " + entry.getValue().getSource()
						+ (orverrided != null ? ", overrided in " + orverrided.getSource() : "") + "\n\n");
		}
		writer.write("\n");
		writer.write("\n");
		for (Map.Entry<String, PropertyDescriptor> entry : overridedProperties.entrySet())
			if (!defaultProperties.containsKey(entry.getKey())) {
				writer.write(entry.getKey() + '=' + entry.getValue().getValue() + "\n");
				if (detail)
					writer.write("#defined in " + entry.getValue().getSource() + "\n\n");
			}
		return NONE;
	}

	public String overrided() throws Exception {
		HttpServletResponse response = ServletActionContext.getResponse();
		response.setContentType("text/plain");
		Writer writer = response.getWriter();
		Map<String, PropertyDescriptor> overridedProperties = applicationContextInspector.getOverridedProperties();
		for (Map.Entry<String, PropertyDescriptor> entry : overridedProperties.entrySet()) {
			writer.write(entry.getKey() + '=' + entry.getValue().getValue() + "\n");
			if (detail)
				writer.write("#defined in " + entry.getValue().getSource() + "\n\n");
		}
		return NONE;
	}

}
