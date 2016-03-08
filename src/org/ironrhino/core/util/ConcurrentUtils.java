package org.ironrhino.core.util;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ConcurrentUtils {

	public static <T> CompletableFuture<T> anyOf(List<? extends CompletableFuture<? extends T>> futures) {
		CompletableFuture<T> f = new CompletableFuture<>();
		Consumer<T> complete = f::complete;
		CompletableFuture.allOf(futures.stream().map(s -> s.thenAccept(complete)).toArray(CompletableFuture<?>[]::new))
				.exceptionally(ex -> {
					f.completeExceptionally(ex);
					return null;
				});
		return f;
	}

}
