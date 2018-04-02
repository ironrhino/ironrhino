package org.ironrhino.core.servlet.handles;

import static org.ironrhino.core.spring.configuration.ApplicationContextPropertiesConditional.ANY;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ironrhino.core.servlet.AccessHandler;
import org.ironrhino.core.spring.configuration.ApplicationContextPropertiesConditional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@ApplicationContextPropertiesConditional(key = "sitemesh.decorator.default", value = ANY)
public class SitemeshHandler extends AccessHandler {

	@Value("${sitemesh.decorator.default:}")
	private String decorator;

	@Override
	public boolean handle(HttpServletRequest request, HttpServletResponse response) {
		if (!decorator.isEmpty())
			request.setAttribute("decorator", decorator);
		return false;
	}

}
