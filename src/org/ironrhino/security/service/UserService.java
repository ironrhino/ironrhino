package org.ironrhino.security.service;

import org.ironrhino.core.remoting.Remoting;
import org.ironrhino.core.spring.security.RemotingUserDetailsService;
import org.ironrhino.security.domain.User;
import org.springframework.core.annotation.Order;

@Remoting(serializationType = "JAVA")
@Order(0)
public interface UserService extends RemotingUserDetailsService<User> {

}
