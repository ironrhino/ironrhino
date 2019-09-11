package org.ironrhino.core.remoting.impl;

import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.ironrhino.core.event.EventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

public class RedisServiceRegistryAdapter {

	protected static ListOperations<String, String> opsForList;
	protected static HashOperations<String, Object, Object> opsForHash;

	@Autowired
	protected EventPublisher eventPublisher;
	@Autowired
	protected RedisServiceRegistry serviceRegistry;
	@Autowired
	protected StringRedisTemplate stringRedisTemplate;

	protected Map<String, List<String>> importedServiceCandidates;
	protected Map<String, Object> exportedServices;
	protected Map<String, String> exportedServiceDescriptions;

	protected static String normalizeHost(String host) {
		int i = host.indexOf('@');
		return i < 0 ? host : host.substring(i + 1);
	}

	@PostConstruct
	public void afterPropertiesSet() {
		importedServiceCandidates = serviceRegistry.getImportedServiceCandidates();
		exportedServices = serviceRegistry.getExportedServices();
		exportedServiceDescriptions = serviceRegistry.getExportedServiceDescriptions();
	}
}
