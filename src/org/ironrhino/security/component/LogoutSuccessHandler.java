package org.ironrhino.security.component;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ironrhino.core.event.EventPublisher;
import org.ironrhino.core.metadata.Scope;
import org.ironrhino.core.spring.security.DefaultLogoutSuccessHandler;
import org.ironrhino.security.event.LogoutEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

public class LogoutSuccessHandler extends DefaultLogoutSuccessHandler {

	@Autowired
	private transient EventPublisher eventPublisher;

	@Override
	public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
			throws IOException, ServletException {
		super.onLogoutSuccess(request, response, authentication);
		if (authentication != null) {
			Object principal = authentication.getPrincipal();
			if (principal instanceof UserDetails)
				eventPublisher.publish(
						new LogoutEvent(((UserDetails) principal).getUsername(), request.getRemoteAddr()), Scope.LOCAL);
		}
	}

}
