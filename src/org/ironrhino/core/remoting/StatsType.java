package org.ironrhino.core.remoting;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.ironrhino.core.model.Displayable;

public enum StatsType implements Displayable {

	SERVER_SIDE("server"), CLIENT_SIDE("client"), CLIENT_FAILED("cfailed");

	private ConcurrentHashMap<String, ConcurrentHashMap<String, AtomicInteger>> countBuffer = new ConcurrentHashMap<String, ConcurrentHashMap<String, AtomicInteger>>();
	private ConcurrentHashMap<String, InvocationSampler> sampleBuffer = new ConcurrentHashMap<String, InvocationSampler>();

	private String namespace;

	private StatsType(String namespace) {
		this.namespace = namespace;
	}

	public ConcurrentHashMap<String, ConcurrentHashMap<String, AtomicInteger>> getCountBuffer() {
		return countBuffer;
	}

	public ConcurrentHashMap<String, InvocationSampler> getSampleBuffer() {
		return sampleBuffer;
	}

	public String getNamespace() {
		return namespace;
	}

	public void increaseCount(String serviceName, String method) {
		ConcurrentHashMap<String, AtomicInteger> map = countBuffer.get(serviceName);
		if (map == null) {
			ConcurrentHashMap<String, AtomicInteger> temp = new ConcurrentHashMap<>();
			temp.put(method, new AtomicInteger());
			map = countBuffer.putIfAbsent(serviceName, temp);
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

	public void collectSample(String host, String serviceName, String method, long time) {
		String service = serviceName + "." + method;
		InvocationSampler sampler = sampleBuffer.get(service);
		if (sampler == null) {
			InvocationSampler temp = new InvocationSampler(host);
			sampler = sampleBuffer.putIfAbsent(service, temp);
			if (sampler == null)
				sampler = temp;
		}
		sampler.add(time);
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