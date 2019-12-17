package org.ironrhino.security.component;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ironrhino.core.service.EntityManager;
import org.ironrhino.core.spring.security.DefaultAuthenticationFailureHandler;
import org.ironrhino.core.struts.I18N;
import org.ironrhino.security.model.LoginRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;

@Component
@Primary
public class LoginRecordAuthenticationFailureHandler extends DefaultAuthenticationFailureHandler {

	@Autowired
	private UsernamePasswordAuthenticationFilter usernamePasswordAuthenticationFilter;

	@Autowired
	private EntityManager<LoginRecord> entityManager;

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException e) throws IOException, ServletException {
		super.onAuthenticationFailure(request, response, e);
		LoginRecord loginRecord = new LoginRecord();
		loginRecord.setUsername(request.getParameter(usernamePasswordAuthenticationFilter.getUsernameParameter()));
		loginRecord.setAddress(request.getRemoteAddr());
		loginRecord.setFailed(true);
		loginRecord.setCause(I18N.getText(e.getClass().getName()));
		if (loginRecord.getUsername() != null)
			entityManager.save(loginRecord);
	}

}
