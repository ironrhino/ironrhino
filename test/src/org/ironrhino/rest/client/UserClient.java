package org.ironrhino.rest.client;

import java.io.InputStream;
import java.util.List;

import org.ironrhino.core.model.ResultPage;
import org.ironrhino.rest.RestStatus;
import org.ironrhino.rest.client.RestClientConfiguration.MyJsonValidator;
import org.ironrhino.security.domain.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@RestApi(restClient = "restClient", requestHeaders = { @RequestHeader(name = "Connection", value = "Keep-Alive") })
@RequestMapping("/user")
public interface UserClient {

	@RequestMapping(value = "/@self", method = RequestMethod.GET)
	User self();

	@RequestMapping(value = "/@all", method = RequestMethod.GET)
	List<User> all();

	@RequestMapping(value = "/@paged", method = RequestMethod.GET)
	ResultPage<User> paged(@RequestParam(defaultValue = "1") Integer pageNo,
			@RequestParam(defaultValue = "10") Integer pageSize);

	@RequestMapping(value = "/@pagedRestResult", method = RequestMethod.GET)
	@JsonPointer("/data")
	ResultPage<User> pagedRestResult(@RequestParam(defaultValue = "1") Integer pageNo,
			@RequestParam(defaultValue = "10") Integer pageSize);

	@RequestMapping(value = "/@pagedRestResult", method = RequestMethod.GET)
	@JsonPointer("/data")
	ResponseEntity<ResultPage<User>> pagedRestResultWithResponseEntity(@RequestParam(defaultValue = "1") Integer pageNo,
			@RequestParam(defaultValue = "10") Integer pageSize);

	@JsonPointer("/result")
	@RequestMapping(value = "/@paged", method = RequestMethod.GET)
	List<User> pagedResult(@RequestParam(defaultValue = "1") Integer pageNo,
			@RequestParam(defaultValue = "10") Integer pageSize);

	@JsonPointer(value = "/result", validator = MyJsonValidator.class)
	@RequestMapping(value = "/@paged", method = RequestMethod.GET)
	List<User> pagedResultWithValidator(@RequestParam(defaultValue = "1") Integer pageNo,
			@RequestParam(defaultValue = "10") Integer pageSize);

	@RequestMapping(value = "/@self", method = RequestMethod.PATCH)
	void patch(@RequestBody User user);

	@RequestMapping(value = "/@self/password", method = RequestMethod.PATCH)
	RestStatus validatePassword(@RequestBody User user);

	@RequestMapping(value = "/{username}", method = RequestMethod.GET)
	User get(String username);

	@RequestMapping(method = RequestMethod.POST)
	void post(@RequestBody User user);

	@RequestMapping(value = "/{username}", method = RequestMethod.PATCH)
	void patch(String username, @RequestBody User user);

	@RequestMapping(value = "/{username}", method = RequestMethod.DELETE)
	void delete(String username);

	@RequestMapping(value = "/{username}/password", method = RequestMethod.PATCH)
	RestStatus validatePassword(@PathVariable String username, @RequestBody User user);

	@RequestMapping(method = RequestMethod.POST)
	void postStream(InputStream is);

	@RequestMapping(method = RequestMethod.POST)
	void postByteArray(byte[] bytes);

	@RequestMapping(value = "/@self", method = RequestMethod.GET)
	InputStream getStream();

}
