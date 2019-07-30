package org.ironrhino.core.throttle;

import static io.github.resilience4j.bulkhead.utils.MetricNames.AVAILABLE_CONCURRENT_CALLS;
import static io.github.resilience4j.bulkhead.utils.MetricNames.DEFAULT_PREFIX;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.ironrhino.core.metrics.Metrics;
import org.ironrhino.core.spring.configuration.ClassPresentConditional;
import org.ironrhino.core.util.CheckedCallable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.utils.BulkheadUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Component
@Getter
@ClassPresentConditional("io.github.resilience4j.bulkhead.Bulkhead")
@Slf4j
public class BulkheadRegistry {

	private final Map<String, Bulkhead> bulkheads = new ConcurrentHashMap<>();

	public <T, E extends Throwable> T executeCheckedCallable(String name, Supplier<BulkheadConfig> configSupplier,
			CheckedCallable<T, E> callable) throws E {
		Bulkhead bh = of(name, configSupplier);
		BulkheadUtils.isCallPermitted(bh);
		try {
			return callable.call();
		} finally {
			bh.onComplete();
		}
	}

	public Bulkhead of(String name, Supplier<BulkheadConfig> configSupplier) {
		return bulkheads.computeIfAbsent(name, key -> {
			Bulkhead bulkhead = Bulkhead.of(key, configSupplier.get());
			if (Metrics.isEnabled()) {
				String prefix = DEFAULT_PREFIX + '.' + key + '.';
				Metrics.gauge(prefix + AVAILABLE_CONCURRENT_CALLS, bulkhead,
						bh -> bh.getMetrics().getAvailableConcurrentCalls());
			}
			return bulkhead;
		});
	}

	public void changeMaxConcurrentCalls(String name, int oldMaxConcurrentCalls, int newMaxConcurrentCalls) {
		Bulkhead bulkhead = bulkheads.get(name);
		if (bulkhead != null) {
			synchronized (bulkhead) {
				if (bulkhead.getBulkheadConfig().getMaxConcurrentCalls() == oldMaxConcurrentCalls) {
					BulkheadConfig oldConfig = bulkhead.getBulkheadConfig();
					bulkhead.changeConfig(BulkheadConfig.custom().maxConcurrentCalls(newMaxConcurrentCalls)
							.maxWaitTime(oldConfig.getMaxWaitTime()).build());
					log.info("Change maxConcurrentCalls of Bulkhead('{}') from {} to {}", name, oldMaxConcurrentCalls,
							newMaxConcurrentCalls);
				} else {
					throw new OptimisticLockingFailureException("State changed, please refresh and retry.");
				}
			}
		}
	}

}
