package org.ironrhino.core.remoting;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.ironrhino.core.model.Displayable;

public enum StatsType implements Displayable {

	SERVER_SIDE("server"), CLIENT_SIDE("client"), CLIENT_FAILED("cfailed");

	private ConcurrentHashMap<String, Map<String, AtomicInteger>> buffer = new ConcurrentHashMap<>();

	private String namespace;

	private StatsType(String namespace) {
		this.namespace = namespace;
	}

	public void emit(String serviceName, String method) {
		Map<String, AtomicInteger> map = buffer.get(serviceName);
		if (map == null) {
			Map<String, AtomicInteger> temp = new ConcurrentHashMap<>();
			temp.put(method, new AtomicInteger());
			map = buffer.putIfAbsent(serviceName, temp);
			if (map == null)
				map = temp;
		}
		AtomicInteger ai = map.get(method);
		if (ai == null) {
			AtomicInteger ai2 = new AtomicInteger();
			ai = map.putIfAbsent(method, ai2);
			if (ai == null)
				ai = ai2;
		}
		ai.incrementAndGet();
	}

	public ConcurrentHashMap<String, Map<String, AtomicInteger>> getBuffer() {
		return buffer;
	}

	public String getNamespace() {
		return namespace;
	}

	@Override
	public String getName() {
		return name();
	}

	@Override
	public String getDisplayName() {
		return Displayable.super.getDisplayName();
	}

	@Override
	public String toString() {
		return getDisplayName();
	}
}