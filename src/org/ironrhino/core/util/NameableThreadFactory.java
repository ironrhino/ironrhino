package org.ironrhino.core.util;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NameableThreadFactory implements ThreadFactory {

	private static final Logger logger = LoggerFactory.getLogger(NameableThreadFactory.class);

	private final AtomicInteger threadNumber = new AtomicInteger(1);
	private final ThreadGroup group;
	private final String namePrefix;
	private final UncaughtExceptionHandler uncaughtExceptionHandler;

	public NameableThreadFactory(String poolName) {
		this(poolName, null, null);
	}

	public NameableThreadFactory(String poolName, String threadGroupName) {
		this(poolName, threadGroupName, null);
	}

	public NameableThreadFactory(String poolName, UncaughtExceptionHandler uncaughtExceptionHandler) {
		this(poolName, null, uncaughtExceptionHandler);
	}

	public NameableThreadFactory(String poolName, String threadGroupName,
			UncaughtExceptionHandler uncaughtExceptionHandler) {
		SecurityManager s = System.getSecurityManager();
		this.group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
		StringBuilder sb = new StringBuilder();
		if (StringUtils.isNotBlank(poolName)) {
			sb.append(poolName);
			sb.append("-");
		}
		if (StringUtils.isNotBlank(threadGroupName)) {
			sb.append(threadGroupName);
			sb.append("-");
		}
		this.namePrefix = sb.toString();
		this.uncaughtExceptionHandler = uncaughtExceptionHandler != null ? uncaughtExceptionHandler : (t, e) -> {
			logger.error(e.getMessage(), e);
		};
	}

	@Override
	public Thread newThread(Runnable r) {
		Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
		if (t.isDaemon())
			t.setDaemon(false);
		if (t.getPriority() != Thread.NORM_PRIORITY)
			t.setPriority(Thread.NORM_PRIORITY);
		if (uncaughtExceptionHandler != null)
			t.setUncaughtExceptionHandler(uncaughtExceptionHandler);
		return t;
	}
}
