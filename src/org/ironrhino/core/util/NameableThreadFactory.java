package org.ironrhino.core.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;

public class NameableThreadFactory implements ThreadFactory {
	private final ThreadGroup group;
	private final AtomicInteger threadNumber = new AtomicInteger(1);
	private final String namePrefix;

	public NameableThreadFactory(String poolName) {
		this(poolName, null);
	}

	public NameableThreadFactory(String poolName, String threadGroupName) {
		SecurityManager s = System.getSecurityManager();
		group = (s != null) ? s.getThreadGroup() : Thread.currentThread()
				.getThreadGroup();
		StringBuilder sb = new StringBuilder();
		if (StringUtils.isNotBlank(poolName)) {
			sb.append(poolName);
			sb.append("-");
		}
		if (StringUtils.isNotBlank(threadGroupName)) {
			sb.append(threadGroupName);
			sb.append("-");
		}
		namePrefix = sb.toString();
	}

	@Override
	public Thread newThread(Runnable r) {
		Thread t = new Thread(group, r, namePrefix
				+ threadNumber.getAndIncrement(), 0);
		if (t.isDaemon())
			t.setDaemon(false);
		if (t.getPriority() != Thread.NORM_PRIORITY)
			t.setPriority(Thread.NORM_PRIORITY);
		return t;
	}
}
