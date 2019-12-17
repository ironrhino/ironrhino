package org.ironrhino.security.component;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ironrhino.core.spring.security.DefaultAuthenticationSuccessHandler;
import org.ironrhino.core.spring.security.password.MultiVersionPasswordEncoder;
import org.ironrhino.security.model.LoginRecord;
import org.ironrhino.security.model.User;
import org.ironrhino.security.service.UserManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@Primary
public class LoginRecordAuthenticationSuccessHandler extends DefaultAuthenticationSuccessHandler {

	@Autowired
	private UserManager userManager;

	@Autowired(required = false)
	private MultiVersionPasswordEncoder multiVersionPasswordEncoder;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws ServletException, IOException {
		super.onAuthenticationSuccess(request, response, authentication);
		Object principal = authentication.getPrincipal();
		String username;
		if (principal instanceof User) {
			User user = (User) principal;
			username = user.getUsername();
			if (multiVersionPasswordEncoder != null && authentication instanceof UsernamePasswordAuthenticationToken
					&& !multiVersionPasswordEncoder.isLastVersion(user.getPassword())) {
				user.setLegiblePassword(authentication.getCredentials().toString());
				userManager.save(user);
			}
		} else if (principal instanceof UserDetails) {
			username = ((UserDetails) principal).getUsername();
		} else {
			username = String.valueOf(principal);
		}
		LoginRecord loginRecord = new LoginRecord();
		loginRecord.setUsername(username);
		loginRecord.setAddress(request.getRemoteAddr());
		userManager.execute(session -> session.save(loginRecord));
	}

}
