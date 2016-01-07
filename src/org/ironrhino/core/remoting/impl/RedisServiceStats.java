package org.ironrhino.core.remoting.impl;

import static org.ironrhino.core.metadata.Profiles.CLOUD;
import static org.ironrhino.core.metadata.Profiles.CLUSTER;
import static org.ironrhino.core.metadata.Profiles.DUAL;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PreDestroy;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.common.model.tuples.Pair;
import org.ironrhino.core.metadata.Trigger;
import org.ironrhino.core.remoting.ServiceStats;
import org.ironrhino.core.spring.configuration.ServiceImplementationConditional;
import org.ironrhino.core.throttle.Mutex;
import org.ironrhino.core.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component("serviceStats")
@ServiceImplementationConditional(profiles = { DUAL, CLUSTER, CLOUD })
public class RedisServiceStats implements ServiceStats {

	protected Logger logger = LoggerFactory.getLogger(getClass());

	private static final String NAMESPACE = "remoting:stats:";
	private static final String NAMESPACE_SERVICES = "remoting:stats:services:";
	private static final String NAMESPACE_SERVER_SIDE = NAMESPACE + "server:";
	private static final String NAMESPACE_CLIENT_SIDE = NAMESPACE + "client:";

	@Value("${serviceStats.archive.days:7}")
	private int days = 7;

	private RedisTemplate<String, String> stringRedisTemplate;

	private ConcurrentHashMap<String, Map<String, AtomicInteger>> serverSideBuffer = new ConcurrentHashMap<>();

	private ConcurrentHashMap<String, Map<String, AtomicInteger>> clientSideBuffer = new ConcurrentHashMap<>();

	@Autowired
	public RedisServiceStats(@Qualifier("stringRedisTemplate") RedisTemplate<String, String> stringRedisTemplate) {
		this.stringRedisTemplate = stringRedisTemplate;
	}

	@Override
	public void emit(String serviceName, String method, long time, boolean failed, boolean clientSide) {
		emit(serviceName, method, clientSide);
	}

	@Override
	public Map<String, Set<String>> getServices() {
		Map<String, Set<String>> map = new TreeMap<>();
		Set<String> serviceKeys = stringRedisTemplate.keys(NAMESPACE_SERVICES + "*");
		for (String serviceKey : serviceKeys) {
			String serviceName = serviceKey.substring(NAMESPACE_SERVICES.length());
			Set<String> methods = stringRedisTemplate.opsForSet().members(serviceKey);
			map.put(serviceName, new TreeSet<>(methods));
		}
		return map;
	}

	@Override
	public long getCount(String serviceName, String method, String key, boolean clientSide) {
		StringBuilder sb = new StringBuilder(clientSide ? NAMESPACE_CLIENT_SIDE : NAMESPACE_SERVER_SIDE);
		sb.append(serviceName).append(".").append(method);
		if (key != null) {
			sb.append(":").append(key);
			String prefix = sb.toString();
			String value = stringRedisTemplate.opsForValue().get(prefix);
			if (value != null) {
				return Long.valueOf(value);
			} else {
				Set<String> keys = stringRedisTemplate.keys(prefix + "*");
				List<String> results = stringRedisTemplate.opsForValue().multiGet(keys);
				long count = 0;
				for (String str : results)
					count += Long.valueOf(str);
				return count;
			}
		} else {
			String value = stringRedisTemplate.opsForValue().get(sb.toString());
			if (value != null)
				return Long.valueOf(value);
			return 0;
		}
	}

	@Override
	public Pair<String, Long> getMaxCount(String serviceName, String method, boolean clientSide) {
		String key = (clientSide ? NAMESPACE_CLIENT_SIDE : NAMESPACE_SERVER_SIDE) + "max";
		String service = serviceName + "." + method;
		String str = (String) stringRedisTemplate.opsForHash().get(key, service);
		if (StringUtils.isNotBlank(str)) {
			String[] arr = str.split(",");
			return new Pair<>(arr[0], Long.valueOf(arr[1]));
		} else {
			String today = DateUtils.formatDate8(new Date());
			long count = getCount(serviceName, method, today, clientSide);
			if (count > 0)
				return new Pair<>(today, count);
		}
		return null;
	}

	private void emit(String serviceName, String method, boolean clientSide) {
		ConcurrentHashMap<String, Map<String, AtomicInteger>> buffer = clientSide ? clientSideBuffer : serverSideBuffer;
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

	@Scheduled(initialDelayString = "${serviceStats.flush.fixedRate:60000}", fixedRateString = "${serviceStats.flush.fixedRate:60000}")
	@PreDestroy
	public void flush() {
		flush(false);
		flush(true);
	}

	private void flush(boolean clientSide) {
		ConcurrentHashMap<String, Map<String, AtomicInteger>> buffer = clientSide ? clientSideBuffer : serverSideBuffer;
		for (Map.Entry<String, Map<String, AtomicInteger>> entry : buffer.entrySet()) {
			stringRedisTemplate.opsForSet().add(NAMESPACE_SERVICES + entry.getKey(),
					entry.getValue().keySet().toArray(new String[0]));
			for (Map.Entry<String, AtomicInteger> entry2 : entry.getValue().entrySet()) {
				AtomicInteger ai = entry2.getValue();
				int count = ai.get();
				if (count > 0) {
					increment(entry.getKey(), entry2.getKey(), count, clientSide);
					ai.addAndGet(-count);
				}
			}
		}
	}

	private void increment(String serviceName, String method, int count, boolean clientSide) {
		Date date = new Date();
		StringBuilder sb = new StringBuilder(clientSide ? NAMESPACE_CLIENT_SIDE : NAMESPACE_SERVER_SIDE);
		sb.append(serviceName).append(".").append(method);
		stringRedisTemplate.opsForValue().increment(sb.toString(), count);
		sb.append(":");
		sb.append(DateUtils.format(date, "yyyyMMddHH"));
		stringRedisTemplate.opsForValue().increment(sb.toString(), count);
	}

	@Trigger
	@Scheduled(cron = "${serviceStats.archive.cron:0 1 0 * * ?}")
	@Mutex
	public void archive() {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_YEAR, -days);
		Date date = cal.getTime();
		String day = DateUtils.formatDate8(date);
		for (Map.Entry<String, Set<String>> entry : getServices().entrySet()) {
			String serviceName = entry.getKey();
			for (String method : entry.getValue()) {
				updateMax(serviceName, method, false);
				updateMax(serviceName, method, true);
				archive(serviceName, method, day, false);
				archive(serviceName, method, day, true);
			}
		}
	}

	private void archive(String serviceName, String method, String day, boolean clientSide) {
		StringBuilder sb = new StringBuilder(clientSide ? NAMESPACE_CLIENT_SIDE : NAMESPACE_SERVER_SIDE);
		sb.append(serviceName).append(".").append(method);
		sb.append(":").append(day);
		String prefix = sb.toString();
		Set<String> keys = stringRedisTemplate.keys(prefix + "*");
		if (!keys.isEmpty()) {
			List<String> results = stringRedisTemplate.opsForValue().multiGet(keys);
			stringRedisTemplate.delete(keys);
			long count = 0;
			for (String str : results)
				count += Long.valueOf(str);
			if (count > 0)
				stringRedisTemplate.opsForValue().set(prefix, String.valueOf(count));
		}
	}

	private void updateMax(String serviceName, String method, boolean clientSide) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_YEAR, -1);
		String yesterday = DateUtils.formatDate8(cal.getTime());
		StringBuilder sb = new StringBuilder(clientSide ? NAMESPACE_CLIENT_SIDE : NAMESPACE_SERVER_SIDE);
		sb.append(serviceName).append(".").append(method);
		sb.append(":").append(yesterday);
		String prefix = sb.toString();
		Set<String> keys = stringRedisTemplate.keys(prefix + "*");
		List<String> results = stringRedisTemplate.opsForValue().multiGet(keys);
		long count = 0;
		for (String str : results)
			count += Long.valueOf(str);
		if (count > 0) {
			String key = (clientSide ? NAMESPACE_CLIENT_SIDE : NAMESPACE_SERVER_SIDE) + "max";
			String service = serviceName + "." + method;
			String str = (String) stringRedisTemplate.opsForHash().get(key, service);
			if (StringUtils.isNotBlank(str)) {
				String[] arr = str.split(",");
				if (Long.valueOf(arr[1]) < count)
					stringRedisTemplate.opsForHash().put(key, service, yesterday + "," + count);
			} else {
				stringRedisTemplate.opsForHash().put(key, service, yesterday + "," + count);
			}
		}
	}

}