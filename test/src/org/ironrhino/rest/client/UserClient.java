package org.ironrhino.rest.client;

import java.io.InputStream;
import java.util.List;

import org.ironrhino.rest.RestStatus;
import org.ironrhino.security.domain.User;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@RestApi(restClient = "restClient")
@RequestMapping("/user")
public interface UserClient {

	@RequestMapping(value = "/@self", method = RequestMethod.GET)
	public User self();

	@RequestMapping(value = "/@all", method = RequestMethod.GET)
	public List<User> all();

	@RequestMapping(value = "/@self", method = RequestMethod.PATCH)
	public void patch(@RequestBody User user);

	@RequestMapping(value = "/@self/password", method = RequestMethod.PATCH)
	public RestStatus validatePassword(@RequestBody User user);

	@RequestMapping(value = "/{username}", method = RequestMethod.GET)
	public User get(String username);

	@RequestMapping(method = RequestMethod.POST)
	public void post(@RequestBody User user);

	@RequestMapping(value = "/{username}", method = RequestMethod.PATCH)
	public void patch(String username, @RequestBody User user);

	@RequestMapping(value = "/{username}", method = RequestMethod.DELETE)
	public void delete(String username);

	@RequestMapping(value = "/{username}/password", method = RequestMethod.PATCH)
	public RestStatus validatePassword(@PathVariable String username, @RequestBody User user);

	@RequestMapping(method = RequestMethod.POST)
	public void postStream(InputStream is);

	@RequestMapping(value = "/@self", method = RequestMethod.GET)
	public InputStream getStream();

}
