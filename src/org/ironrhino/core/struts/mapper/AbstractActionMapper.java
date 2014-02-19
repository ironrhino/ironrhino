package org.ironrhino.core.struts.mapper;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts2.dispatcher.mapper.ActionMapper;
import org.apache.struts2.dispatcher.mapper.ActionMapping;
import org.ironrhino.core.util.RequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractActionMapper implements ActionMapper {

	public static final String ID = "id";

	protected Logger log = LoggerFactory.getLogger(getClass());

	public static String getUri(HttpServletRequest request) {
		String uri = RequestUtils.getRequestUri(request);
		if (uri.endsWith("/"))
			uri += "index";
		return uri;
	}

	@Override
	public ActionMapping getMappingFromActionName(String actionName) {
		ActionMapping mapping = new ActionMapping();
		mapping.setName(actionName);
		return parseActionName(mapping);
	}

	protected ActionMapping parseActionName(ActionMapping mapping) {
		if (mapping.getName() == null) {
			return mapping;
		}
		// handle "name!method" convention.
		String name = mapping.getName();
		int exclamation = name.lastIndexOf("!");
		if (exclamation != -1) {
			mapping.setName(name.substring(0, exclamation));
			mapping.setMethod(name.substring(exclamation + 1));
		}
		return mapping;
	}

}
