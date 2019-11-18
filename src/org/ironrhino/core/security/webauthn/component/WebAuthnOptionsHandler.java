package org.ironrhino.core.security.webauthn.component;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.security.webauthn.WebAuthnService;
import org.ironrhino.core.security.webauthn.internal.Utils;
import org.ironrhino.core.servlet.AccessHandler;
import org.ironrhino.core.spring.configuration.ApplicationContextPropertiesConditional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@ApplicationContextPropertiesConditional(key = "webAuthn.enabled", value = "true")
@Component
@Order(0)
public class WebAuthnOptionsHandler extends AccessHandler {

	@Autowired
	protected WebAuthnService webAuthnService;

	@Override
	public String getPattern() {
		return "/webAuthnOptions";
	}

	@Override
	public boolean handle(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		Object value;
		String username = request.getParameter("username");
		if (StringUtils.isBlank(username)) {
			Map<String, Object> map = new HashMap<>();
			map.put("actionErrors", Collections.singletonList("Missing username"));
			value = map;
		} else {
			value = webAuthnService.buildRequestOptions(username);
		}
		Utils.JSON_OBJECTMAPPER.writeValue(response.getOutputStream(), value);
		return true;
	}

}
