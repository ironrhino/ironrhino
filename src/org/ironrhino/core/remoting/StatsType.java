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
		ConcurrentHashMap<String, AtomicInteger> serviceMap = countBuffer.computeIfAbsent(serviceName, key -> {
			ConcurrentHashMap<String, AtomicInteger> methodMap = new ConcurrentHashMap<>();
			methodMap.put(method, new AtomicInteger());
			return methodMap;
		});
		AtomicInteger ai = serviceMap.computeIfAbsent(method, key -> new AtomicInteger());
		ai.incrementAndGet();
	}

	public void collectSample(String host, String serviceName, String method, long time) {
		String service = serviceName + '.' + method;
		InvocationSampler sampler = sampleBuffer.computeIfAbsent(service, key -> new InvocationSampler(host));
		sampler.add(time);
	}

	@Override
	public String toString() {
		return getDisplayName();
	}
}