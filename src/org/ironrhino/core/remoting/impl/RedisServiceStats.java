package org.ironrhino.core.remoting.impl;

import static org.ironrhino.core.metadata.Profiles.CLOUD;
import static org.ironrhino.core.metadata.Profiles.CLUSTER;
import static org.ironrhino.core.metadata.Profiles.DUAL;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.common.model.tuples.Pair;
import org.ironrhino.core.metadata.Trigger;
import org.ironrhino.core.remoting.InvocationSample;
import org.ironrhino.core.remoting.InvocationSampler;
import org.ironrhino.core.remoting.InvocationWarning;
import org.ironrhino.core.remoting.ServiceRegistry;
import org.ironrhino.core.remoting.ServiceStats;
import org.ironrhino.core.remoting.StatsType;
import org.ironrhino.core.spring.configuration.ServiceImplementationConditional;
import org.ironrhino.core.throttle.Mutex;
import org.ironrhino.core.util.DateUtils;
import org.ironrhino.core.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.BoundZSetOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component("serviceStats")
@ServiceImplementationConditional(profiles = { DUAL, CLUSTER, CLOUD })
public class RedisServiceStats implements ServiceStats {

	protected Logger logger = LoggerFactory.getLogger(getClass());

	private static final String NAMESPACE = "remoting:stats:";
	private static final String NAMESPACE_SERVICES = NAMESPACE + "services:";
	private static final String NAMESPACE_SAMPLES = NAMESPACE + "samples:";
	private static final String KEY_HOTSPOTS = NAMESPACE + "hotspots";
	private static final String KEY_WARNINGS = NAMESPACE + "warnings";

	private CopyOnWriteArrayList<String> warningBuffer = new CopyOnWriteArrayList<String>();

	@Autowired(required = false)
	private ServiceRegistry serviceRegistry;

	@Value("${serviceStats.archive.days:7}")
	private int days = 7;

	@Value("${serviceStats.responseTimeThreshold:5000}")
	public long responseTimeThreshold = 5000;

	@Value("${serviceStats.maxWarningsSize:100}")
	public long maxWarningsSize = 100;

	@Value("${serviceStats.maxSamplesSize:20}")
	public long maxSamplesSize = 20;

	private RedisTemplate<String, String> stringRedisTemplate;

	private BoundZSetOperations<String, String> hotspotsOperations;
	private BoundListOperations<String, String> warningsOperations;

	@Autowired
	public RedisServiceStats(@Qualifier("stringRedisTemplate") RedisTemplate<String, String> stringRedisTemplate) {
		this.stringRedisTemplate = stringRedisTemplate;
	}

	@PostConstruct
	public void init() {
		hotspotsOperations = stringRedisTemplate.boundZSetOps(KEY_HOTSPOTS);
		warningsOperations = stringRedisTemplate.boundListOps(KEY_WARNINGS);
	}

	@Override
	public void serverSideEmit(String serviceName, String method, long time) {
		emit(null, null, serviceName, method, time, StatsType.SERVER_SIDE);
	}

	@Override
	public void clientSideEmit(String target, String serviceName, String method, long time, boolean failed) {
		emit(serviceRegistry != null ? serviceRegistry.getLocalHost() : null, target, serviceName, method, time,
				failed ? StatsType.CLIENT_FAILED : StatsType.CLIENT_SIDE);
	}

	protected void emit(String source, String target, String serviceName, String method, long time, StatsType type) {
		type.increaseCount(serviceName, method);
		type.collectSample(serviceRegistry != null ? serviceRegistry.getLocalHost() : null, serviceName, method, time);
		if (type == StatsType.CLIENT_FAILED || type == StatsType.CLIENT_SIDE && time > responseTimeThreshold) {
			InvocationWarning warning = new InvocationWarning(source, target, serviceName + "." + method, time,
					type == StatsType.CLIENT_FAILED);
			warningBuffer.add(JsonUtils.toJson(warning));
		}
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
	public long getCount(String service, String key, StatsType type) {
		StringBuilder sb = new StringBuilder(getNameSpace(type));
		sb.append(service);
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
	public Pair<String, Long> getMaxCount(String service, StatsType type) {
		String key = getNameSpace(type) + "max";
		String str = (String) stringRedisTemplate.opsForHash().get(key, service);
		if (StringUtils.isNotBlank(str)) {
			String[] arr = str.split(",");
			return new Pair<>(arr[0], Long.valueOf(arr[1]));
		} else {
			String today = DateUtils.formatDate8(new Date());
			long count = getCount(service, today, type);
			if (count > 0)
				return new Pair<>(today, count);
		}
		return null;
	}

	@Override
	public Map<String, Long> findHotspots(int limit) {
		Set<TypedTuple<String>> result = hotspotsOperations.reverseRangeWithScores(0, limit - 1);
		Map<String, Long> map = new LinkedHashMap<>();
		for (TypedTuple<String> tt : result)
			map.put(tt.getValue(), tt.getScore().longValue());
		return map;
	}

	@Override
	public List<InvocationWarning> getWarnings() {
		List<String> list = warningsOperations.range(0, -1);
		List<InvocationWarning> results = new ArrayList<>(list.size());
		try {
			for (String str : list)
				results.add(JsonUtils.fromJson(str, InvocationWarning.class));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return results;
	}

	@Override
	public List<InvocationSample> getSamples(String service, StatsType type) {
		List<String> list = stringRedisTemplate.opsForList()
				.range(NAMESPACE_SAMPLES + type.getNamespace() + ":" + service, 0, -1);
		List<InvocationSample> results = new ArrayList<>(list.size());
		try {
			for (String str : list)
				results.add(JsonUtils.fromJson(str, InvocationSample.class));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return results;
	}

	@Scheduled(initialDelayString = "${serviceStats.flush.fixedRate:60000}", fixedRateString = "${serviceStats.flush.fixedRate:60000}")
	@PreDestroy
	public void flush() {
		if (!warningBuffer.isEmpty()) {
			warningsOperations.leftPushAll(warningBuffer.toArray(new String[0]));
			warningBuffer.clear();
			long size = warningsOperations.size();
			if (size > maxWarningsSize)
				warningsOperations.trim(0, maxWarningsSize - 1);
		}
		for (StatsType type : StatsType.values()) {
			for (Map.Entry<String, InvocationSampler> entry : type.getSampleBuffer().entrySet()) {
				InvocationSample sample = entry.getValue().peekAndReset();
				if (sample.getCount() > 0) {
					String key = NAMESPACE_SAMPLES + type.getNamespace() + ":" + entry.getKey();
					stringRedisTemplate.opsForList().leftPush(key, JsonUtils.toJson(sample));
					long size = stringRedisTemplate.opsForList().size(key);
					if (size > maxSamplesSize)
						stringRedisTemplate.opsForList().trim(key, 0, maxSamplesSize - 1);
				}
			}
		}
		for (StatsType type : StatsType.values())
			flush(type);
	}

	private void flush(StatsType type) {
		ConcurrentHashMap<String, Map<String, AtomicInteger>> buffer = type.getCountBuffer();
		for (Map.Entry<String, Map<String, AtomicInteger>> entry : buffer.entrySet()) {
			stringRedisTemplate.opsForSet().add(NAMESPACE_SERVICES + entry.getKey(),
					entry.getValue().keySet().toArray(new String[0]));
			for (Map.Entry<String, AtomicInteger> entry2 : entry.getValue().entrySet()) {
				AtomicInteger ai = entry2.getValue();
				int count = ai.get();
				if (count > 0) {
					increment(entry.getKey(), entry2.getKey(), count, type);
					ai.addAndGet(-count);
				}
			}
		}
	}

	private void increment(String serviceName, String method, int count, StatsType type) {
		Date date = new Date();
		StringBuilder sb = new StringBuilder(getNameSpace(type));
		sb.append(serviceName).append(".").append(method);
		stringRedisTemplate.opsForValue().increment(sb.toString(), count);
		sb.append(":");
		sb.append(DateUtils.format(date, "yyyyMMddHH"));
		stringRedisTemplate.opsForValue().increment(sb.toString(), count);
		if (type == StatsType.SERVER_SIDE)
			hotspotsOperations.incrementScore(serviceName + "." + method, count);
	}

	@Trigger
	@Scheduled(cron = "${serviceStats.archive.cron:0 1 0 * * ?}")
	@Mutex
	public void archive() {
		stringRedisTemplate.delete(KEY_HOTSPOTS);
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_YEAR, -days);
		Date date = cal.getTime();
		String day = DateUtils.formatDate8(date);
		for (Map.Entry<String, Set<String>> entry : getServices().entrySet()) {
			String serviceName = entry.getKey();
			for (String method : entry.getValue()) {
				for (StatsType type : StatsType.values()) {
					updateMax(serviceName, method, type);
					archive(serviceName, method, day, type);
				}
			}
		}
	}

	private void archive(String serviceName, String method, String day, StatsType type) {
		StringBuilder sb = new StringBuilder(getNameSpace(type));
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

	private void updateMax(String serviceName, String method, StatsType type) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_YEAR, -1);
		String yesterday = DateUtils.formatDate8(cal.getTime());
		StringBuilder sb = new StringBuilder(getNameSpace(type));
		sb.append(serviceName).append(".").append(method);
		sb.append(":").append(yesterday);
		String prefix = sb.toString();
		Set<String> keys = stringRedisTemplate.keys(prefix + "*");
		List<String> results = stringRedisTemplate.opsForValue().multiGet(keys);
		long count = 0;
		for (String str : results)
			count += Long.valueOf(str);
		if (count > 0) {
			String key = getNameSpace(type) + "max";
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

	private static String getNameSpace(StatsType type) {
		return NAMESPACE + type.getNamespace() + ":";
	}

}