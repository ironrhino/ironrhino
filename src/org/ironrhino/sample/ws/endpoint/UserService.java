package org.ironrhino.sample.ws.endpoint;

import javax.jws.WebService;

@WebService(serviceName = "UserService", targetNamespace = "http://sample.ironrhino.org")
public interface UserService {

	public String suggestUsername(String username);

}