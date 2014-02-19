package org.ironrhino.core.struts.mapper;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts2.dispatcher.mapper.ActionMapping;

public interface ActionMappingMatcher {

	public ActionMapping tryMatch(HttpServletRequest request,
			DefaultActionMapper actionMapper);

}
