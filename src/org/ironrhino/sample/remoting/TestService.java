package org.ironrhino.sample.remoting;

import java.util.List;

import org.ironrhino.core.remoting.Remoting;
import org.springframework.security.core.userdetails.UserDetails;

@Remoting
public interface TestService {

	public void ping();

	public void throwException(String message) throws Exception;

	public String echo();

	public String echo(String str);

	public List<String> echoList(List<String> list);

	public List<String[]> echoListWithArray(List<String[]> list);

	public int countAndAdd(List<String> list, int param);

	public String[] echoArray(String[] arr);

	public UserDetails loadUserByUsername(String username);

}
