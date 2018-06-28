package org.ironrhino.sample.remoting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.security.domain.User;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class TestServiceImpl implements TestService {

	@Override
	public void ping() {

	}

	@Override
	public void throwException(String message) throws Exception {
		throw new IllegalArgumentException(message);
	}

	@Override
	public String echo() {
		return "";
	}

	@Override
	public String echo(String str) {
		return str;
	}

	@Override
	public List<String> echoList(List<String> list) {
		return list;
	}

	@Override
	public List<String[]> echoListWithArray(List<String[]> list) {
		return list;
	}

	@Override
	public int countAndAdd(List<String> list, int para2) {
		return list.size() + para2;
	}

	@Override
	public String[] echoArray(String[] arr) {
		return arr;
	}

	@Override
	public UserDetails loadUserByUsername(String username) {
		if (username == null)
			return null;
		User user = new User();
		user.setUsername(username);
		user.setAuthorities(AuthorityUtils.createAuthorityList("test"));
		return user;
	}

	@Override
	public List<UserDetails> search(String keyword) {
		if (keyword == null)
			return null;
		if (StringUtils.isBlank(keyword))
			return Collections.emptyList();
		List<UserDetails> list = new ArrayList<>();
		list.add(loadUserByUsername(keyword));
		return list;
	}

	@Override
	public Optional<UserDetails> loadOptionalUserByUsername(String username) {
		if (username == null)
			throw new IllegalArgumentException("username shouldn't be null");
		if (username.isEmpty())
			return Optional.empty();
		User user = new User();
		user.setUsername(username);
		user.setAuthorities(AuthorityUtils.createAuthorityList("test"));
		return Optional.of(user);
	}

}
