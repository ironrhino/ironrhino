package org.ironrhino.core.cache.impl;

import static org.ironrhino.core.metadata.Profiles.CLUSTER;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.command.BinaryCommandFactory;
import net.rubyeye.xmemcached.impl.KetamaMemcachedSessionLocator;
import net.rubyeye.xmemcached.utils.AddrUtil;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.cache.CacheManager;
import org.ironrhino.core.metadata.PostPropertiesReset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component("cacheManager")
@Profile(CLUSTER)
public class MemcachedCacheManager implements CacheManager {

	private Logger log = LoggerFactory.getLogger(getClass());

	@Value("${memcached.serverAddress:localhost:11211}")
	private String serverAddress;

	private MemcachedClient memcached;

	private boolean rebuild; // reserve last set

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
		Assert.hasLength(serverAddress);
		MemcachedClientBuilder builder = new XMemcachedClientBuilder(
				AddrUtil.getAddresses(serverAddress));
		builder.setSessionLocator(new KetamaMemcachedSessionLocator());
		builder.setCommandFactory(new BinaryCommandFactory());
		return builder.build();
	}

	@Override
	public void put(String key, Object value, int timeToLive,
			TimeUnit timeUnit, String namespace) {
		put(key, value, -1, timeToLive, timeUnit, namespace);
	}

	@Override
	public void put(String key, Object value, int timeToIdle, int timeToLive,
			TimeUnit timeUnit, String namespace) {
		if (key == null || value == null)
			return;
		try {
			memcached.setWithNoReply(generateKey(key, namespace),
					(int) timeUnit.toSeconds(timeToLive), value);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
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
	public Object get(String key, String namespace, int timeToLive,
			TimeUnit timeUnit) {
		if (key == null)
			return null;
		if (timeToLive <= 0)
			return get(key, namespace);
		try {
			return memcached.getAndTouch(generateKey(key, namespace),
					(int) timeUnit.toSeconds(timeToLive));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
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
	public void mput(Map<String, Object> map, int timeToLive,
			TimeUnit timeUnit, String namespace) {
		if (map == null)
			return;
		for (Map.Entry<String, Object> entry : map.entrySet())
			put(entry.getKey(), entry.getValue(), timeToLive, timeUnit,
					namespace);
	}

	@Override
	public Map<String, Object> mget(Collection<String> keys, String namespace) {
		if (keys == null)
			return null;
		List<String> list = new ArrayList<String>();
		for (String key : keys)
			list.add(generateKey(key, namespace));
		try {
			return memcached.get(list);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}

	@Override
	public void mdelete(Collection<String> keys, String namespace) {
		if (keys == null)
			return;
		for (String key : keys)
			delete(key, namespace);
	}

	@Override
	public boolean containsKey(String key, String namespace) {
		if (key == null)
			return false;
		try {
			return (memcached.get(generateKey(key, namespace)) != null);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return false;
		}
	}

	@Override
	public boolean putIfAbsent(String key, Object value, int timeToLive,
			TimeUnit timeUnit, String namespace) {
		try {
			return memcached.add(generateKey(key, namespace),
					(int) timeUnit.toSeconds(timeToLive), value);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return false;
		}
	}

	@Override
	public long increment(String key, long delta, int timeToLive,
			TimeUnit timeUnit, String namespace) {
		try {
			return memcached.incr(generateKey(key, namespace), delta, delta,
					2000, (int) timeUnit.toSeconds(timeToLive));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return -1;
		}
	}

	private String generateKey(String key, String namespace) {
		if (StringUtils.isNotBlank(namespace)) {
			StringBuilder sb = new StringBuilder(namespace.length()
					+ key.length() + 1);
			sb.append(namespace);
			sb.append(':');
			sb.append(key);
			return sb.toString();
		} else {
			return key;
		}

	}

	@Override
	public boolean supportsTimeToIdle() {
		return false;
	}

	@Override
	public boolean supportsUpdateTimeToLive() {
		return true;
	}

}
