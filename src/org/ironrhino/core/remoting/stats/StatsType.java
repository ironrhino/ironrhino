package org.ironrhino.core.remoting.stats;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.ironrhino.core.model.Displayable;

public enum StatsType implements Displayable {

	SERVER_SIDE("server"), CLIENT_SIDE("client"), CLIENT_FAILED("cfailed");

	private Map<String, Map<String, AtomicInteger>> countBuffer = new ConcurrentHashMap<>();
	private Map<String, InvocationSampler> sampleBuffer = new ConcurrentHashMap<>();

	private String namespace;

	private StatsType(String namespace) {
		this.namespace = namespace;
	}

	public Map<String, Map<String, AtomicInteger>> getCountBuffer() {
		return countBuffer;
	}

	public Map<String, InvocationSampler> getSampleBuffer() {
		return sampleBuffer;
	}

	public String getNamespace() {
		return namespace;
	}

	public void increaseCount(String serviceName, String method) {
		Map<String, AtomicInteger> serviceMap = countBuffer.computeIfAbsent(serviceName, key -> {
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