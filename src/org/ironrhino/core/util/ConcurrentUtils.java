package org.ironrhino.core.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ConcurrentUtils {

	public static <T> CompletableFuture<T> anyOfFutures(List<? extends CompletableFuture<? extends T>> futures) {
		CompletableFuture<T> cf = new CompletableFuture<>();
		CompletableFuture
				.allOf(futures.stream().map(f -> f.thenAccept(cf::complete)).toArray(CompletableFuture<?>[]::new))
				.exceptionally(ex -> {
					cf.completeExceptionally(ex);
					return null;
				});
		return cf;
	}

	public static <T> CompletableFuture<List<T>> allOfFutures(List<? extends CompletableFuture<? extends T>> futures) {
		List<T> result = new ArrayList<>(futures.size());
		CompletableFuture<List<T>> cf = new CompletableFuture<>();
		CompletableFuture
				.allOf(futures.stream().map(s -> s.thenAccept(result::add)).toArray(CompletableFuture<?>[]::new))
				.exceptionally(ex -> {
					cf.completeExceptionally(ex);
					return null;
				}).thenAccept(v -> cf.complete(result));
		return cf;
	}

}
