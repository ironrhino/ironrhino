package org.ironrhino.core.metrics;

import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerNotification;
import javax.management.MalformedObjectNameException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.FunctionTimer;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.TimeGauge;
import io.micrometer.core.instrument.binder.MeterBinder;

public class TomcatMetrics implements MeterBinder {
	private final MBeanServer mBeanServer;
	private final Iterable<Tag> tags;

	public TomcatMetrics(Iterable<Tag> tags) {
		this(tags, ManagementFactory.getPlatformMBeanServer());
	}

	public TomcatMetrics(Iterable<Tag> tags, MBeanServer mBeanServer) {
		this.tags = tags;
		this.mBeanServer = mBeanServer;
	}

	public static void monitor(MeterRegistry registry, String... tags) {
		monitor(registry, Tags.of(tags));
	}

	public static void monitor(MeterRegistry registry, Iterable<Tag> tags) {
		new TomcatMetrics(tags).bindTo(registry);
	}

	@Override
	public void bindTo(MeterRegistry reg) {
		registerGlobalRequestMetrics(reg);
		registerThreadPoolMetrics(reg);
	}

	private void registerThreadPoolMetrics(MeterRegistry registry) {
		registerMetricsEventually("type", "ThreadPool", (name, allTags) -> {
			Gauge.builder("tomcat.threads.busy", mBeanServer,
					s -> safeDouble(() -> s.getAttribute(name, "currentThreadsBusy"))).tags(allTags).register(registry);

			Gauge.builder("tomcat.threads.current", mBeanServer,
					s -> safeDouble(() -> s.getAttribute(name, "currentThreadCount"))).tags(allTags).register(registry);
		});
	}

	private void registerGlobalRequestMetrics(MeterRegistry registry) {
		registerMetricsEventually("type", "GlobalRequestProcessor", (name, allTags) -> {
			FunctionCounter
					.builder("tomcat.global.sent", mBeanServer,
							s -> safeDouble(() -> s.getAttribute(name, "bytesSent")))
					.tags(allTags).baseUnit("bytes").register(registry);

			FunctionCounter
					.builder("tomcat.global.received", mBeanServer,
							s -> safeDouble(() -> s.getAttribute(name, "bytesReceived")))
					.tags(allTags).baseUnit("bytes").register(registry);

			FunctionCounter.builder("tomcat.global.error", mBeanServer,
					s -> safeDouble(() -> s.getAttribute(name, "errorCount"))).tags(allTags).register(registry);

			FunctionTimer
					.builder("tomcat.global.request", mBeanServer,
							s -> safeLong(() -> s.getAttribute(name, "requestCount")),
							s -> safeDouble(() -> s.getAttribute(name, "processingTime")), TimeUnit.MILLISECONDS)
					.tags(allTags).register(registry);

			TimeGauge.builder("tomcat.global.request.max", mBeanServer, TimeUnit.MILLISECONDS,
					s -> safeDouble(() -> s.getAttribute(name, "maxTime"))).tags(allTags).register(registry);
		});
	}

	/**
	 * If the MBean already exists, register metrics immediately. Otherwise register
	 * an MBean registration listener with the MBeanServer and register metrics
	 * when/if the MBean becomes available.
	 */
	private void registerMetricsEventually(String key, String value, BiConsumer<ObjectName, Iterable<Tag>> perObject) {
		try {
			Set<ObjectName> objs = mBeanServer.queryNames(new ObjectName("Catalina:" + key + "=" + value + ",*"), null);
			if (!objs.isEmpty()) {
				// MBean is present, so we can register metrics now.
				objs.forEach(o -> perObject.accept(o, Tags.concat(tags, nameTag(o))));
				return;
			}
		} catch (MalformedObjectNameException e) {
			// should never happen
			throw new RuntimeException("Error registering Tomcat JMX based metrics", e);
		}

		// MBean isn't yet registered, so we'll set up a notification to wait for them
		// to be present and register
		// metrics later.
		NotificationListener notificationListener = (notification, handback) -> {
			MBeanServerNotification mbs = (MBeanServerNotification) notification;
			ObjectName obj = mbs.getMBeanName();
			perObject.accept(obj, Tags.concat(tags, nameTag(obj)));
		};

		NotificationFilter filter = (NotificationFilter) notification -> {
			if (!MBeanServerNotification.REGISTRATION_NOTIFICATION.equals(notification.getType()))
				return false;

			// we can safely downcast now
			ObjectName obj = ((MBeanServerNotification) notification).getMBeanName();
			return obj.getDomain().equals("Catalina") && obj.getKeyProperty(key).equals(value);

		};

		try {
			mBeanServer.addNotificationListener(MBeanServerDelegate.DELEGATE_NAME, notificationListener, filter, null);
		} catch (InstanceNotFoundException e) {
			// should never happen
			throw new RuntimeException("Error registering MBean listener", e);
		}
	}

	private double safeDouble(Callable<Object> callable) {
		try {
			return Double.parseDouble(callable.call().toString());
		} catch (Exception e) {
			return 0.0;
		}
	}

	private long safeLong(Callable<Object> callable) {
		try {
			return Long.parseLong(callable.call().toString());
		} catch (Exception e) {
			return 0;
		}
	}

	private Iterable<Tag> nameTag(ObjectName name) {
		if (name.getKeyProperty("name") != null) {
			return Tags.of("name", name.getKeyProperty("name").replaceAll("\"", ""));
		} else {
			return Collections.emptyList();
		}
	}
}
