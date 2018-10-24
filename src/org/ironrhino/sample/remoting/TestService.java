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

	public boolean echo(boolean value);

	public int echo(int value);

	public Long echo(Long value);

	public Date echo(Date value);

	public Scope echo(@NotNull Scope value);

	public User echo(@Valid User value);

	public List<String> echoList(List<String> list);

	public List<String[]> echoListWithArray(List<String[]> list);

	public int countAndAdd(List<String> list, int param);

	public String[] echoArray(String[] arr);

	public UserDetails loadUserByUsername(String username);

	public List<UserDetails> search(String keyword);

	public Optional<UserDetails> loadOptionalUserByUsername(String username);

	public Future<UserDetails> loadFutureUserByUsername(String username, FutureType futureType);

	public ListenableFuture<UserDetails> loadListenableFutureUserByUsername(String username);

	public Callable<UserDetails> loadCallableUserByUsername(String username);

}
