package org.ironrhino.security.service;

import org.ironrhino.core.remoting.Remoting;
import org.ironrhino.core.spring.security.RemotingUserDetailsService;
import org.springframework.core.annotation.Order;

@Remoting
@Order(0)
public interface UserService extends RemotingUserDetailsService {

}
