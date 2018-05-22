package org.ironrhino.core.metrics;

import java.util.concurrent.TimeUnit;
import java.util.function.ToDoubleFunction;
import java.util.function.ToLongFunction;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.FunctionTimer;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.TimeGauge;
import io.micrometer.core.instrument.binder.MeterBinder;

public class JettyMetrics implements MeterBinder {

	private final Object statisticsHandler;

	private Iterable<Tag> tags;

	public JettyMetrics(Object statisticsHandler, Iterable<Tag> tags) {
		this.tags = tags;
		this.statisticsHandler = statisticsHandler;
	}

	public static void monitor(MeterRegistry meterRegistry, Object statisticsHandler, String... tags) {
		monitor(meterRegistry, statisticsHandler, Tags.of(tags));
	}

	public static void monitor(MeterRegistry meterRegistry, Object statisticsHandler, Iterable<Tag> tags) {
		new JettyMetrics(statisticsHandler, tags).bindTo(meterRegistry);
	}

	@Override
	public void bindTo(MeterRegistry registry) {
		bindTimer(registry, "jetty.requests", "Request duration", toLongFunction("getRequests"),
				toDoubleFunction("getRequestTimeTotal"));
		bindTimer(registry, "jetty.dispatched", "Dispatch duration", toLongFunction("getDispatched"),
				toDoubleFunction("getDispatchedTimeTotal"));
		bindCounter(registry, "jetty.async.requests", "Total number of async requests",
				toDoubleFunction("getAsyncRequests"));
		bindCounter(registry, "jetty.async.dispatches",
				"Total number of requests that have been asynchronously dispatched",
				toDoubleFunction("getAsyncDispatches"));
		bindCounter(registry, "jetty.async.expires", "Total number of async requests that have expired",
				toDoubleFunction("getExpires"));
		FunctionCounter.builder("jetty.responses.size", statisticsHandler, toDoubleFunction("getResponsesBytesTotal"))
				.description("Total number of bytes across all responses").baseUnit("bytes").register(registry);

		bindGauge(registry, "jetty.requests.active", "Number of requests currently active",
				toDoubleFunction("getRequestsActive"));
		bindGauge(registry, "jetty.dispatched.active", "Number of dispatches currently active",
				toDoubleFunction("getDispatchedActive"));
		bindGauge(registry, "jetty.dispatched.active.max", "Maximum number of active dispatches being handled",
				toDoubleFunction("getDispatchedActiveMax"));

		bindTimeGauge(registry, "jetty.dispatched.time.max", "Maximum time spent in dispatch handling",
				toDoubleFunction("getDispatchedTimeMax"));

		bindGauge(registry, "jetty.async.requests.waiting", "Currently waiting async requests",
				toDoubleFunction("getAsyncRequestsWaiting"));
		bindGauge(registry, "jetty.async.requests.waiting.max", "Maximum number of waiting async requests",
				toDoubleFunction("getAsyncRequestsWaitingMax"));

		bindTimeGauge(registry, "jetty.request.time.max", "Maximum time spent handling requests",
				toDoubleFunction("getRequestTimeMax"));
		bindTimeGauge(registry, "jetty.stats", "Time stats have been collected for", toDoubleFunction("getStatsOnMs"));

		bindStatusCounters(registry);
	}

	private void bindStatusCounters(MeterRegistry registry) {
		for (int i = 1; i <= 5; i++)
			buildStatusCounter(registry, i + "xx", toDoubleFunction("getResponses" + i + "xx"));
	}

	private void bindGauge(MeterRegistry registry, String name, String description,
			ToDoubleFunction<Object> valueFunction) {
		Gauge.builder(name, statisticsHandler, valueFunction).tags(tags).description(description).register(registry);
	}

	private void bindTimer(MeterRegistry registry, String name, String desc, ToLongFunction<Object> countFunc,
			ToDoubleFunction<Object> consumer) {
		FunctionTimer.builder(name, statisticsHandler, countFunc, consumer, TimeUnit.MILLISECONDS).tags(tags)
				.description(desc).register(registry);
	}

	private void bindTimeGauge(MeterRegistry registry, String name, String desc, ToDoubleFunction<Object> consumer) {
		TimeGauge.builder(name, statisticsHandler, TimeUnit.MILLISECONDS, consumer).tags(tags).description(desc)
				.register(registry);
	}

	private void bindCounter(MeterRegistry registry, String name, String desc, ToDoubleFunction<Object> consumer) {
		FunctionCounter.builder(name, statisticsHandler, consumer).tags(tags).description(desc).register(registry);
	}

	private void buildStatusCounter(MeterRegistry registry, String status, ToDoubleFunction<Object> consumer) {
		FunctionCounter.builder("jetty.responses", statisticsHandler, consumer).tags(tags)
				.description("Number of requests with response status").tags("status", status).register(registry);
	}

	private static ToDoubleFunction<Object> toDoubleFunction(String method) {
		return obj -> {
			try {
				return ((Number) obj.getClass().getMethod(method).invoke(obj)).doubleValue();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		};

	}

	private static ToLongFunction<Object> toLongFunction(String method) {
		return obj -> {
			try {
				return ((Number) obj.getClass().getMethod(method).invoke(obj)).longValue();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		};

	}
}
