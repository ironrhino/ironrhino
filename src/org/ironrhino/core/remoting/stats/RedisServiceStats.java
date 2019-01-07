package org.ironrhino.core.remoting.stats;

import static org.ironrhino.core.metadata.Profiles.CLOUD;
import static org.ironrhino.core.metadata.Profiles.CLUSTER;
import static org.ironrhino.core.metadata.Profiles.DUAL;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.metadata.Trigger;
import org.ironrhino.core.metrics.Metrics;
import org.ironrhino.core.model.Tuple;
import org.ironrhino.core.remoting.ServiceRegistry;
import org.ironrhino.core.spring.configuration.PriorityQualifier;
import org.ironrhino.core.spring.configuration.ServiceImplementationConditional;
import org.ironrhino.core.util.DateUtils;
import org.ironrhino.core.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.BoundZSetOperations;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
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

	private CopyOnWriteArrayList<String> warningBuffer = new CopyOnWriteArrayList<>();

	@Autowired(required = false)
	private ServiceRegistry serviceRegistry;

	@Value("${serviceStats.archiveDays:7}")
	private int archiveDays = 7;

	@Value("${serviceStats.responseTimeThreshold:5000}")
	private long responseTimeThreshold = 5000;

	@Value("${serviceStats.maxWarningsSize:100}")
	private long maxWarningsSize = 100;

	@Value("${serviceStats.maxSamplesSize:20}")
	private long maxSamplesSize = 20;

	@Autowired
	@Qualifier("stringRedisTemplate")
	@PriorityQualifier({ "remotingStringRedisTemplate", "globalStringRedisTemplate" })
	private StringRedisTemplate remotingStringRedisTemplate;

	private BoundZSetOperations<String, String> hotspotsOperations;
	private BoundListOperations<String, String> warningsOperations;

	@PostConstruct
	public void afterPropertiesSet() {
		hotspotsOperations = remotingStringRedisTemplate.boundZSetOps(KEY_HOTSPOTS);
		warningsOperations = remotingStringRedisTemplate.boundListOps(KEY_WARNINGS);
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
		String service = serviceName + "." + method;
		if (type == StatsType.SERVER_SIDE) {
			Metrics.recordTimer("remoting.server.calls", time, TimeUnit.MILLISECONDS, "service", service);
		} else {
			Metrics.recordTimer("remoting.client.calls", time, TimeUnit.MILLISECONDS, "service", service, "failed",
					String.valueOf(type == StatsType.CLIENT_FAILED));
		}
		type.increaseCount(serviceName, method);
		type.collectSample(serviceRegistry != null ? serviceRegistry.getLocalHost() : null, serviceName, method, time);
		if (type == StatsType.CLIENT_FAILED || type == StatsType.CLIENT_SIDE && time > responseTimeThreshold) {
			InvocationWarning warning = new InvocationWarning(source, target, service, time,
					type == StatsType.CLIENT_FAILED);
			warningBuffer.add(JsonUtils.toJson(warning));
		}
	}

	@Override
	public Map<String, Set<String>> getServices() {
		Map<String, Set<String>> map = new TreeMap<>();
		Set<String> serviceKeys = remotingStringRedisTemplate.<Set<String>>execute((RedisConnection conn) -> {
			Set<String> set = new HashSet<>();
			Cursor<byte[]> cursor = conn
					.scan(new ScanOptions.ScanOptionsBuilder().match(NAMESPACE_SERVICES + "*").count(100).build());
			while (cursor.hasNext())
				set.add((String) remotingStringRedisTemplate.getKeySerializer().deserialize(cursor.next()));
			return set;
		});
		if (serviceKeys == null)
			return Collections.emptyMap();
		for (String serviceKey : serviceKeys) {
			String serviceName = serviceKey.substring(NAMESPACE_SERVICES.length());
			Set<String> methods = remotingStringRedisTemplate.opsForSet().members(serviceKey);
			if (methods != null)
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
			String value = remotingStringRedisTemplate.opsForValue().get(prefix);
			if (value != null) {
				return Long.valueOf(value);
			} else {
				Set<String> keys = remotingStringRedisTemplate.<Set<String>>execute((RedisConnection conn) -> {
					Set<String> set = new HashSet<>();
					Cursor<byte[]> cursor = conn
							.scan(new ScanOptions.ScanOptionsBuilder().match(prefix + "*").count(100).build());
					while (cursor.hasNext())
						set.add((String) remotingStringRedisTemplate.getKeySerializer().deserialize(cursor.next()));
					return set;
				});
				if (keys == null)
					return 0;
				List<String> results = remotingStringRedisTemplate.opsForValue().multiGet(keys);
				if (results == null)
					return 0;
				long count = 0;
				for (String str : results)
					count += Long.valueOf(str);
				return count;
			}
		} else {
			String value = remotingStringRedisTemplate.opsForValue().get(sb.toString());
			if (value != null)
				return Long.valueOf(value);
			return 0;
		}
	}

	@Override
	public Tuple<String, Long> getMaxCount(String service, StatsType type) {
		String key = getNameSpace(type) + "max";
		String str = (String) remotingStringRedisTemplate.opsForHash().get(key, service);
		if (StringUtils.isNotBlank(str)) {
			String[] arr = str.split(",");
			return Tuple.of(arr[0], Long.valueOf(arr[1]));
		} else {
			String today = DateUtils.formatDate8(new Date());
			long count = getCount(service, today, type);
			if (count > 0)
				return Tuple.of(today, count);
		}
		return null;
	}

	@Override
	public Map<String, Long> findHotspots(int limit) {
		Set<TypedTuple<String>> result = hotspotsOperations.reverseRangeWithScores(0, limit - 1);
		if (result == null)
			return Collections.emptyMap();
		Map<String, Long> map = new LinkedHashMap<>();
		for (TypedTuple<String> tt : result) {
			Double score = tt.getScore();
			map.put(tt.getValue(), score != null ? score.longValue() : 0);
		}
		return map;
	}

	@Override
	public List<InvocationWarning> getWarnings() {
		List<String> list = warningsOperations.range(0, -1);
		if (list == null)
			return Collections.emptyList();
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
		List<String> list = remotingStringRedisTemplate.opsForList()
				.range(NAMESPACE_SAMPLES + type.getNamespace() + ':' + service, 0, -1);
		if (list == null)
			return Collections.emptyList();
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
			warningsOperations.leftPushAll(warningBuffer.toArray(new String[warningBuffer.size()]));
			warningBuffer.clear();
			Long size = warningsOperations.size();
			if (size != null && size > maxWarningsSize)
				warningsOperations.trim(0, maxWarningsSize - 1);
		}
		for (StatsType type : StatsType.values()) {
			for (Map.Entry<String, InvocationSampler> entry : type.getSampleBuffer().entrySet()) {
				InvocationSample sample = entry.getValue().peekAndReset();
				if (sample.getCount() > 0) {
					String key = NAMESPACE_SAMPLES + type.getNamespace() + ':' + entry.getKey();
					remotingStringRedisTemplate.opsForList().leftPush(key, JsonUtils.toJson(sample));
					Long size = remotingStringRedisTemplate.opsForList().size(key);
					if (size != null && size > maxSamplesSize)
						remotingStringRedisTemplate.opsForList().trim(key, 0, maxSamplesSize - 1);
				}
			}
		}
		for (StatsType type : StatsType.values())
			flush(type);
	}

	private void flush(StatsType type) {
		ConcurrentHashMap<String, ConcurrentHashMap<String, AtomicInteger>> buffer = type.getCountBuffer();
		for (Map.Entry<String, ConcurrentHashMap<String, AtomicInteger>> entry : buffer.entrySet()) {
			remotingStringRedisTemplate.opsForSet().add(NAMESPACE_SERVICES + entry.getKey(),
					entry.getValue().keySet().toArray(new String[entry.getValue().size()]));
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
		remotingStringRedisTemplate.opsForValue().increment(sb.toString(), count);
		sb.append(":");
		sb.append(DateUtils.format(date, "yyyyMMddHH"));
		remotingStringRedisTemplate.opsForValue().increment(sb.toString(), count);
		if (type == StatsType.SERVER_SIDE)
			hotspotsOperations.incrementScore(serviceName + '.' + method, count);
	}

	@Trigger
	@Scheduled(cron = "${serviceStats.archive.cron:0 1 0 * * ?}")
	public void archive() {
		String lockName = "lock:serviceStats.archive()";
		Boolean b = remotingStringRedisTemplate.opsForValue().setIfAbsent(lockName, "");
		if (b != null && b)
			try {
				remotingStringRedisTemplate.expire(lockName, 5, TimeUnit.MINUTES);
				remotingStringRedisTemplate.delete(KEY_HOTSPOTS);
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.DAY_OF_YEAR, -archiveDays);
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
			} finally {
				remotingStringRedisTemplate.delete(lockName);
			}
	}

	private void archive(String serviceName, String method, String day, StatsType type) {
		StringBuilder sb = new StringBuilder(getNameSpace(type));
		sb.append(serviceName).append(".").append(method);
		sb.append(":").append(day);
		String prefix = sb.toString();
		Set<String> keys = remotingStringRedisTemplate.<Set<String>>execute((RedisConnection conn) -> {
			Set<String> set = new HashSet<>();
			Cursor<byte[]> cursor = conn
					.scan(new ScanOptions.ScanOptionsBuilder().match(prefix + "*").count(100).build());
			while (cursor.hasNext())
				set.add((String) remotingStringRedisTemplate.getKeySerializer().deserialize(cursor.next()));
			return set;
		});
		if (keys == null)
			return;
		if (!keys.isEmpty()) {
			List<String> results = remotingStringRedisTemplate.opsForValue().multiGet(keys);
			remotingStringRedisTemplate.delete(keys);
			if (results == null)
				return;
			long count = 0;
			for (String str : results)
				if (StringUtils.isNumeric(str))
					count += Long.valueOf(str);
			if (count > 0)
				remotingStringRedisTemplate.opsForValue().set(prefix, String.valueOf(count));
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
		Set<String> keys = remotingStringRedisTemplate.<Set<String>>execute((RedisConnection conn) -> {
			Set<String> set = new HashSet<>();
			Cursor<byte[]> cursor = conn
					.scan(new ScanOptions.ScanOptionsBuilder().match(prefix + "*").count(100).build());
			while (cursor.hasNext())
				set.add((String) remotingStringRedisTemplate.getKeySerializer().deserialize(cursor.next()));
			return set;
		});
		if (keys == null)
			return;
		List<String> results = remotingStringRedisTemplate.opsForValue().multiGet(keys);
		if (results == null)
			return;
		long count = 0;
		for (String str : results)
			count += Long.valueOf(str);
		if (count > 0) {
			String key = getNameSpace(type) + "max";
			String service = serviceName + '.' + method;
			String str = (String) remotingStringRedisTemplate.opsForHash().get(key, service);
			if (StringUtils.isNotBlank(str)) {
				String[] arr = str.split(",");
				if (Long.valueOf(arr[1]) < count)
					remotingStringRedisTemplate.opsForHash().put(key, service, yesterday + ',' + count);
			} else {
				remotingStringRedisTemplate.opsForHash().put(key, service, yesterday + ',' + count);
			}
		}
	}

	private static String getNameSpace(StatsType type) {
		return NAMESPACE + type.getNamespace() + ":";
	}

}