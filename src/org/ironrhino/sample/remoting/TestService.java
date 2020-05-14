package org.ironrhino.sample.remoting;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.ironrhino.core.metadata.Scope;
import org.ironrhino.core.remoting.Remoting;
import org.ironrhino.security.domain.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.concurrent.ListenableFuture;

import lombok.Value;

@Remoting
public interface TestService extends GenericService<UserDetails> {

	enum FutureType {
		RUNNABLE, COMPLETABLE, LISTENABLE;
	}

	@Value
	class Immutable implements Serializable {
		private static final long serialVersionUID = 1L;
		private long id;
		private String name;
	}

	default String defaultEcho(String value) {
		return echo(value);
	}

	void ping();

	void throwException(String message) throws Exception;

	String echo();

	String echo(String value);

	boolean echoBoolean(boolean value);

	int echoInt(int value);

	Long echoLong(Long value);

	Date echoDate(Date value);

	Scope echoScope(@NotNull Scope value);

	Immutable echoImmutable(Immutable value);

	User echoUser(@Valid User value);

	UserDetails echoUserDetails(@Valid UserDetails value);

	List<String> echoList(List<String> list);

	List<String[]> echoListWithArray(List<String[]> list);

	int countAndAdd(List<String> list, int param);

	String[] echoArray(String[] arr);

	User loadUserByUsername(String username);

	UserDetails loadUserDetailsByUsername(String username);

	List<User> searchUser(String keyword);

	List<? extends UserDetails> searchUserDetails(String keyword);

	Optional<User> loadOptionalUserByUsername(String username);

	Optional<? extends UserDetails> loadOptionalUserDetailsByUsername(String username);

	Future<User> loadFutureUserByUsername(String username, @NotNull FutureType futureType);

	Future<? extends UserDetails> loadFutureUserDetailsByUsername(String username, @NotNull FutureType futureType);

	ListenableFuture<User> loadListenableFutureUserByUsername(String username);

	ListenableFuture<? extends UserDetails> loadListenableFutureUserDetailsByUsername(String username);

	CompletableFuture<User> loadCompletableFutureUserByUsername(String username);

	CompletableFuture<? extends UserDetails> loadCompletableFutureUserDetailsByUsername(String username);
	
	CompletionStage<? extends UserDetails> loadCompletionStageUserDetailsByUsername(String username);

	Callable<User> loadCallableUserByUsername(String username);

	Callable<? extends UserDetails> loadCallableUserDetailsByUsername(String username);

}
