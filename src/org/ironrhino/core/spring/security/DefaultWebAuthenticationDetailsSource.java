package org.ironrhino.core.spring.security;

import javax.servlet.http.HttpServletRequest;

import org.ironrhino.core.spring.configuration.ResourcePresentConditional;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;

@Component
@ResourcePresentConditional("classpath*:resources/spring/applicationContext-security*.xml")
public class DefaultWebAuthenticationDetailsSource extends WebAuthenticationDetailsSource {

	@Override
	public WebAuthenticationDetails buildDetails(HttpServletRequest context) {
		return new DefaultWebAuthenticationDetails(context);
	}

}
