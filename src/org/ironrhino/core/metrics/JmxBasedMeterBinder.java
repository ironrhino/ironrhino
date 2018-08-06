package org.ironrhino.core.metrics;

import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerNotification;
import javax.management.MalformedObjectNameException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;

public abstract class JmxBasedMeterBinder implements MeterBinder {

	protected final MBeanServer mBeanServer;

	protected final Iterable<Tag> tags;

	public JmxBasedMeterBinder() {
		this(Collections.emptyList());
	}

	public JmxBasedMeterBinder(Iterable<Tag> tags) {
		this.tags = tags;
		this.mBeanServer = ManagementFactory.getPlatformMBeanServer();
	}

	/**
	 * If the MBean already exists, register metrics immediately. Otherwise register
	 * an MBean registration listener with the MBeanServer and register metrics
	 * when/if the MBean becomes available.
	 */
	protected void registerMetricsEventually(String domain, String type,
			BiConsumer<ObjectName, Iterable<Tag>> perObject) {
		try {
			Set<ObjectName> objs = mBeanServer.queryNames(new ObjectName(domain + ":type=" + type + ",*"), null);
			if (!objs.isEmpty()) {
				// MBean is present, so we can register metrics now.
				objs.forEach(o -> perObject.accept(o, Tags.concat(tags, nameTag(o))));
				return;
			}
		} catch (MalformedObjectNameException e) {
			// should never happen
			throw new RuntimeException("Error registering JMX based metrics", e);
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
			return obj.getDomain().equals(domain) && obj.getKeyProperty("type").equals(type);

		};

		try {
			mBeanServer.addNotificationListener(MBeanServerDelegate.DELEGATE_NAME, notificationListener, filter, null);
		} catch (InstanceNotFoundException e) {
			// should never happen
			throw new RuntimeException("Error registering MBean listener", e);
		}
	}

	protected Iterable<Tag> nameTag(ObjectName objectName) {
		return Collections.emptyList();
	}

	protected double safeDouble(Callable<Object> callable) {
		try {
			double value = Double.parseDouble(callable.call().toString());
			if (Double.isNaN(value) || Double.isInfinite(value)) {
				return 0.0;
			}
			return value;
		} catch (Exception e) {
			return 0.0;
		}
	}

	protected long safeLong(Callable<Object> callable) {
		try {
			return Long.parseLong(callable.call().toString());
		} catch (Exception e) {
			return 0;
		}
	}

}
