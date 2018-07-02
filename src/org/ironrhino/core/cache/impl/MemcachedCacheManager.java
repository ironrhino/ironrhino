package org.ironrhino.core.cache.impl;

import static org.ironrhino.core.metadata.Profiles.CLUSTER;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.cache.CacheManager;
import org.ironrhino.core.metadata.PostPropertiesReset;
import org.ironrhino.core.spring.configuration.ServiceImplementationConditional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.command.BinaryCommandFactory;
import net.rubyeye.xmemcached.impl.KetamaMemcachedSessionLocator;
import net.rubyeye.xmemcached.utils.AddrUtil;

@Component("cacheManager")
@ServiceImplementationConditional(profiles = CLUSTER)
@Slf4j
public class MemcachedCacheManager implements CacheManager {

	@Value("${memcached.serverAddress:localhost:11211}")
	private String serverAddress;

	@Setter
	@Value("${memcached.useFstSerialization:false}")
	private boolean useFstSerialization;

	@Setter
	@Value("${memcached.timeout:2000}")
	private long timeout = 2000;

	private volatile MemcachedClient memcached;

	private volatile boolean rebuild; // reserve last set

	public void setServerAddress(String val) {
		if (val != null && serverAddress != null && !val.equals(serverAddress))
			rebuild = true;
		serverAddress = val;
	}

	@PostConstruct
	public void init() {
		try {
			memcached = build(serverAddress);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	@PostPropertiesReset
	public void rebuild() throws IOException {
		if (rebuild) {
			rebuild = false;
			MemcachedClient temp = memcached;
			memcached = build(serverAddress);
			temp.shutdown();
		}
	}

	@PreDestroy
	public void destroy() {
		if (memcached != null)
			try {
				memcached.shutdown();
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
	}

	private MemcachedClient build(String serverAddress) throws IOException {
		Assert.hasLength(serverAddress, "serverAddress shouldn't be blank");
		MemcachedClientBuilder builder = new XMemcachedClientBuilder(AddrUtil.getAddresses(serverAddress));
		builder.setSessionLocator(new KetamaMemcachedSessionLocator());
		builder.setCommandFactory(new BinaryCommandFactory());
		if (useFstSerialization)
			builder.setTranscoder(new FstTranscoder());
		return builder.build();
	}

	@Override
	public void put(String key, Object value, int timeToLive, TimeUnit timeUnit, String namespace) {
		try {
			memcached.setWithNoReply(generateKey(key, namespace), (int) timeUnit.toSeconds(timeToLive), value);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	@Override
	public void putWithTti(String key, Object value, int timeToIdle, TimeUnit timeUnit, String namespace) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean exists(String key, String namespace) {
		if (key == null)
			return false;
		try {
			return memcached.get(generateKey(key, namespace)) != null;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return false;
		}
	}

	@Override
	public Object get(String key, String namespace) {
		if (key == null)
			return null;
		try {
			return memcached.get(generateKey(key, namespace));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}

	@Override
	public Object getWithTti(String key, String namespace, int timeToIdle, TimeUnit timeUnit) {
		if (key == null)
			return null;
		if (timeToIdle <= 0)
			return get(key, namespace);
		try {
			return memcached.getAndTouch(generateKey(key, namespace), (int) timeUnit.toSeconds(timeToIdle));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}

	@Override
	public long ttl(String key, String namespace) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setTtl(String key, String namespace, int timeToLive, TimeUnit timeUnit) {
		try {
			memcached.touch(generateKey(key, namespace), (int) timeUnit.toSeconds(timeToLive));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	@Override
	public void delete(String key, String namespace) {
		if (key == null)
			return;
		try {
			memcached.deleteWithNoReply(generateKey(key, namespace));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	@Override
	public void mput(Map<String, Object> map, int timeToLive, TimeUnit timeUnit, String namespace) {
		if (map == null)
			return;
		for (Map.Entry<String, Object> entry : map.entrySet())
			put(entry.getKey(), entry.getValue(), timeToLive, timeUnit, namespace);
	}

	@Override
	public Map<String, Object> mget(Set<String> keys, String namespace) {
		if (keys == null)
			return null;
		keys = keys.stream().filter(StringUtils::isNotBlank).collect(Collectors.toCollection(HashSet::new));
		try {
			Map<String, Object> map = memcached
					.get(keys.stream().map(key -> generateKey(key, namespace)).collect(Collectors.toList()));
			Map<String, Object> result = new LinkedHashMap<>();
			for (String key : keys)
				result.put(key, map.get(generateKey(key, namespace)));
			return result;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}

	@Override
	public void mdelete(Set<String> keys, String namespace) {
		if (keys == null)
			return;
		for (String key : keys)
			if (StringUtils.isNotBlank(key))
				delete(key, namespace);
	}

	@Override
	public boolean putIfAbsent(String key, Object value, int timeToLive, TimeUnit timeUnit, String namespace) {
		try {
			return memcached.add(generateKey(key, namespace),
					timeToLive > 0 ? (int) timeUnit.toSeconds(timeToLive) : Integer.MAX_VALUE, value);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public long increment(String key, long delta, int timeToLive, TimeUnit timeUnit, String namespace) {
		if (delta == 0)
			throw new IllegalArgumentException("delta should not be 0");
		try {
			if (timeToLive == 0)
				return memcached.incr(generateKey(key, namespace), delta);
			else
				return memcached.incr(generateKey(key, namespace), delta, delta, this.timeout,
						timeToLive > 0 ? (int) timeUnit.toSeconds(timeToLive) : Integer.MAX_VALUE);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private String generateKey(String key, String namespace) {
		if (key == null)
			throw new IllegalArgumentException("key should not be null");
		if (StringUtils.isNotBlank(namespace)) {
			StringBuilder sb = new StringBuilder(namespace.length() + key.length() + 1);
			sb.append(namespace);
			sb.append(':');
			sb.append(key);
			return sb.toString();
		} else {
			return key;
		}

	}

	@Override
	public boolean supportsTti() {
		return false;
	}

	@Override
	public boolean supportsGetTtl() {
		return false;
	}

	@Override
	public boolean supportsUpdateTtl() {
		return true;
	}

}
