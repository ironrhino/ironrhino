package org.ironrhino.core.throttle;

import static io.github.resilience4j.bulkhead.utils.MetricNames.AVAILABLE_CONCURRENT_CALLS;
import static io.github.resilience4j.bulkhead.utils.MetricNames.DEFAULT_PREFIX;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.ironrhino.core.metrics.Metrics;
import org.ironrhino.core.spring.configuration.ClassPresentConditional;
import org.ironrhino.core.util.ThrowableCallable;
import org.springframework.stereotype.Component;

import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.utils.BulkheadUtils;
import lombok.Getter;

@Component
@Getter
@ClassPresentConditional("io.github.resilience4j.bulkhead.Bulkhead")
public class BulkheadRegistry {

	private final Map<String, io.github.resilience4j.bulkhead.Bulkhead> bulkheads = new ConcurrentHashMap<>();

	public <T, E extends Throwable> T executeThrowableCallable(String name, Supplier<BulkheadConfig> configSupplier,
			ThrowableCallable<T, E> callable) throws E {
		io.github.resilience4j.bulkhead.Bulkhead bh = of(name, configSupplier);
		BulkheadUtils.isCallPermitted(bh);
		try {
			return callable.call();
		} finally {
			bh.onComplete();
		}
	}

	public io.github.resilience4j.bulkhead.Bulkhead of(String name, Supplier<BulkheadConfig> configSupplier) {
		return getBulkheads().computeIfAbsent(name, key -> {
			io.github.resilience4j.bulkhead.Bulkhead bulkhead = io.github.resilience4j.bulkhead.Bulkhead.of(key,
					configSupplier.get());
			if (Metrics.isEnabled()) {
				String prefix = DEFAULT_PREFIX + '.' + key + '.';
				Metrics.gauge(prefix + AVAILABLE_CONCURRENT_CALLS, bulkhead,
						bh -> bh.getMetrics().getAvailableConcurrentCalls());
			}
			return bulkhead;
		});
	}

}
