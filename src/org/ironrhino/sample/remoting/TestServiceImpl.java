package org.ironrhino.sample.remoting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import javax.annotation.PreDestroy;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.metadata.Scope;
import org.ironrhino.security.domain.User;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

@Service
public class TestServiceImpl implements TestService {

	private ExecutorService es = Executors.newCachedThreadPool();

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
	public boolean echo(boolean bool) {
		return bool;
	}

	@Override
	public int echo(int integer) {
		return integer;
	}

	@Override
	public Long echo(Long value) {
		return value;
	}

	@Override
	public Date echo(Date value) {
		return value;
	}

	@Override
	public Scope echo(Scope value) {
		return value;
	}

	@Override
	public User echo(User value) {
		return value;
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

	@Override
	public Future<UserDetails> loadFutureUserByUsername(String username, FutureType futureType) {
		if (username == null)
			throw new IllegalArgumentException("username shouldn't be null");
		Supplier<UserDetails> sup = () -> {
			if (StringUtils.isBlank(username))
				throw new IllegalArgumentException("username shouldn't be blank");
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			User user = new User();
			user.setUsername(username);
			user.setAuthorities(AuthorityUtils.createAuthorityList("test"));
			return user;
		};
		switch (futureType) {
		case COMPLETABLE:
			return CompletableFuture.supplyAsync(sup);
		case LISTENABLE:
			SettableListenableFuture<UserDetails> future = new SettableListenableFuture<>();
			if (StringUtils.isBlank(username))
				future.setException(new IllegalArgumentException("username shouldn't be blank"));
			else
				future.set(sup.get());
			return future;
		default:
			return es.submit(sup::get);
		}
	}

	@Override
	public ListenableFuture<UserDetails> loadListenableFutureUserByUsername(String username) {
		Supplier<UserDetails> sup = () -> {
			if (StringUtils.isBlank(username))
				throw new IllegalArgumentException("username shouldn't be blank");
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			User user = new User();
			user.setUsername(username);
			user.setAuthorities(AuthorityUtils.createAuthorityList("test"));
			return user;
		};
		SettableListenableFuture<UserDetails> future = new SettableListenableFuture<>();
		if (StringUtils.isBlank(username))
			future.setException(new IllegalArgumentException("username shouldn't be blank"));
		else
			future.set(sup.get());
		return future;
	}

	@Override
	public Callable<UserDetails> loadCallableUserByUsername(String username) {
		if (username == null)
			throw new IllegalArgumentException("username shouldn't be null");
		return () -> {
			if (StringUtils.isBlank(username))
				throw new IllegalArgumentException("username shouldn't be blank");
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			User user = new User();
			user.setUsername(username);
			user.setAuthorities(AuthorityUtils.createAuthorityList("test"));
			return user;
		};
	}

	@PreDestroy
	private void destroy() {
		es.shutdown();
	}

}
