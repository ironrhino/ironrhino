package org.ironrhino.sample.ws.endpoint;

import javax.jws.WebMethod;
import javax.jws.WebService;

import org.ironrhino.security.service.UserManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

@Component
@WebService(serviceName = "UserService", targetNamespace = "http://sample.ironrhino.org")
public class UserServiceEndpoint extends SpringBeanAutowiringSupport implements UserService {

	@Autowired
	private UserManager userManager;

	@WebMethod
	@Override
	public String suggestUsername(String username) {
		return userManager.suggestUsername(username);
	}

}