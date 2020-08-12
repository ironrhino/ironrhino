package org.ironrhino.core.struts.mapper;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts2.dispatcher.mapper.ActionMapping;

@FunctionalInterface
public interface ActionMappingMatcher {

	ActionMapping tryMatch(HttpServletRequest request, DefaultActionMapper actionMapper);

}
