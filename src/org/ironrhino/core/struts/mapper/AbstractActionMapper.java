package org.ironrhino.core.struts.mapper;

import org.apache.struts2.dispatcher.mapper.ActionMapper;
import org.apache.struts2.dispatcher.mapper.ActionMapping;

public abstract class AbstractActionMapper implements ActionMapper {

	public static final String ID = "id";

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
