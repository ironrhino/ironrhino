package org.ironrhino.security.component;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.ironrhino.core.spring.security.DefaultAuthenticationSuccessHandler;
import org.ironrhino.core.spring.security.password.MultiVersionPasswordEncoder;
import org.ironrhino.core.util.RequestUtils;
import org.ironrhino.security.model.LoginRecord;
import org.ironrhino.security.model.User;
import org.ironrhino.security.service.UserManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@Primary
public class AuthenticationSuccessHandler extends
		DefaultAuthenticationSuccessHandler {

	@Autowired
	private UserManager userManager;

	@Autowired(required = false)
	private MultiVersionPasswordEncoder multiVersionPasswordEncoder;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request,
			HttpServletResponse response, Authentication authentication)
			throws ServletException, IOException {
		super.onAuthenticationSuccess(request, response, authentication);
		User user = (User) authentication.getPrincipal();
		if (multiVersionPasswordEncoder != null
				&& authentication instanceof UsernamePasswordAuthenticationToken
				&& !multiVersionPasswordEncoder.isLastVersion(user
						.getPassword())) {
			user.setLegiblePassword(authentication.getCredentials().toString());
			userManager.save(user);
		}
		LoginRecord loginRecord = new LoginRecord();
		loginRecord.setUsername(user.getUsername());
		loginRecord.setAddress(RequestUtils.getRemoteAddr(request));
		save(loginRecord);
	}

	private void save(final LoginRecord loginRecord) {
		userManager.execute(new HibernateCallback<LoginRecord>() {
			@Override
			public LoginRecord doInHibernate(Session session)
					throws HibernateException, SQLException {
				session.save(loginRecord);
				return null;
			}
		});
	}
}
