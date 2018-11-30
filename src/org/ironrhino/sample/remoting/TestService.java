package org.ironrhino.sample.remoting;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.ironrhino.core.metadata.Scope;
import org.ironrhino.core.remoting.Remoting;
import org.ironrhino.security.domain.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.concurrent.ListenableFuture;

@Remoting
public interface TestService {

	enum FutureType {
		RUNNABLE, COMPLETABLE, LISTENABLE;
	}

	public default String defaultEcho(String value) {
		return echo(value);
	}

	public void ping();

	public void throwException(String message) throws Exception;

	public String echo();

	public String echo(String value);

	public boolean echoBoolean(boolean value);

	public int echoInt(int value);

	public Long echoLong(Long value);

	public Date echoDate(Date value);

	public Scope echoScope(@NotNull Scope value);

	public User echoUser(@Valid User value);

	public UserDetails echoUserDetails(@Valid UserDetails value);

	public List<String> echoList(List<String> list);

	public List<String[]> echoListWithArray(List<String[]> list);

	public int countAndAdd(List<String> list, int param);

	public String[] echoArray(String[] arr);

	public User loadUserByUsername(String username);

	public UserDetails loadUserDetailsByUsername(String username);

	public List<User> searchUser(String keyword);

	public List<? extends UserDetails> searchUserDetails(String keyword);

	public Optional<User> loadOptionalUserByUsername(String username);

	public Optional<? extends UserDetails> loadOptionalUserDetailsByUsername(String username);

	public Future<User> loadFutureUserByUsername(String username, FutureType futureType);

	public Future<? extends UserDetails> loadFutureUserDetailsByUsername(String username, FutureType futureType);

	public ListenableFuture<User> loadListenableFutureUserByUsername(String username);

	public ListenableFuture<? extends UserDetails> loadListenableFutureUserDetailsByUsername(String username);

	public Callable<User> loadCallableUserByUsername(String username);

	public Callable<? extends UserDetails> loadCallableUserDetailsByUsername(String username);

}
