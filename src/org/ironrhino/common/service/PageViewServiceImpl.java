package org.ironrhino.common.service;

import java.net.URI;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.common.util.Location;
import org.ironrhino.common.util.LocationUtils;
import org.ironrhino.core.metadata.Trigger;
import org.ironrhino.core.model.Tuple;
import org.ironrhino.core.spring.configuration.PriorityQualifier;
import org.ironrhino.core.throttle.Mutex;
import org.ironrhino.core.util.DateUtils;
import org.ironrhino.core.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.HyperLogLogOperations;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class PageViewServiceImpl implements PageViewService {

	public static final String KEY_PAGE_VIEW = "pv:";

	public static final String KEY_HYPERLOGLOG_SUFFIX = "_hll";

	@Autowired(required = false)
	@Qualifier("stringRedisTemplate")
	@PriorityQualifier
	private StringRedisTemplate pageViewStringRedisTemplate;

	@Override
	public void put(Date date, String ip, String url, String sessionId, String username, String referer) {
		if (pageViewStringRedisTemplate == null)
			return;
		String domain = null;
		domain = URI.create(url).getHost();
		if (domain.startsWith("www."))
			domain = domain.substring(4);
		addDomain(domain);
		addPageView(date, null);
		addPageView(date, domain);
		String day = DateUtils.formatDate8(date);
		addUnique(day, "uip", ip, null);
		addUnique(day, "uip", ip, domain);
		boolean added1 = addUnique(day, "usid", sessionId, null);
		boolean added2 = addUnique(day, "usid", sessionId, domain);
		addUnique(day, "uu", username, null);
		addUnique(day, "uu", username, domain);
		addUrlVisit(day, url, null);
		addUrlVisit(day, url, domain);
		analyzeReferer(day, url, referer, null);
		analyzeReferer(day, url, referer, domain);
		if (added1)
			analyzeLocation(day, ip, null);
		if (added2)
			analyzeLocation(day, ip, domain);
	}

	private void addDomain(String domain) {
		pageViewStringRedisTemplate.opsForSet().add(KEY_PAGE_VIEW + "domains", domain);
	}

	private void addPageView(Date date, String domain) {
		StringBuilder sb = new StringBuilder(KEY_PAGE_VIEW);
		if (StringUtils.isNotBlank(domain))
			sb.append(domain).append(":");
		sb.append("pv");
		pageViewStringRedisTemplate.opsForValue().increment(sb.toString(), 1);
		sb.append(":");
		String prefix = sb.toString();
		String key = DateUtils.format(date, "yyyyMMddHH");
		pageViewStringRedisTemplate.opsForValue().increment(prefix + key, 1);
	}

	private void addUrlVisit(String day, String url, String domain) {
		StringBuilder sb = new StringBuilder(KEY_PAGE_VIEW);
		if (StringUtils.isNotBlank(domain))
			sb.append(domain).append(":");
		sb.append("url");
		pageViewStringRedisTemplate.opsForZSet().incrementScore(sb.toString(), url, 1);
		sb.append(":");
		sb.append(day);
		pageViewStringRedisTemplate.opsForZSet().incrementScore(sb.toString(), url, 1);
	}

	private void analyzeReferer(String day, String url, String referer, String domain) {
		if (StringUtils.isBlank(referer) || RequestUtils.isSameOrigin(url, referer))
			return;

		StringBuilder sb = new StringBuilder(KEY_PAGE_VIEW);
		if (StringUtils.isNotBlank(domain))
			sb.append(domain).append(":");
		sb.append("fr");
		pageViewStringRedisTemplate.opsForZSet().incrementScore(sb.toString(), referer, 1);
		sb.append(":");
		sb.append(day);
		pageViewStringRedisTemplate.opsForZSet().incrementScore(sb.toString(), referer, 1);

		String[] result = parseSearchUrl(referer);
		if (result == null)
			return;
		String searchengine = result[0];
		String keyword = result[1];
		if (StringUtils.isNotBlank(searchengine)) {
			sb = new StringBuilder(KEY_PAGE_VIEW);
			if (StringUtils.isNotBlank(domain))
				sb.append(domain).append(":");
			sb.append("se");
			pageViewStringRedisTemplate.opsForZSet().incrementScore(sb.toString(), searchengine, 1);
			sb.append(":");
			sb.append(day);
			pageViewStringRedisTemplate.opsForZSet().incrementScore(sb.toString(), searchengine, 1);
		}
		if (StringUtils.isNotBlank(keyword)) {
			sb = new StringBuilder(KEY_PAGE_VIEW);
			if (StringUtils.isNotBlank(domain))
				sb.append(domain).append(":");
			sb.append("kw");
			pageViewStringRedisTemplate.opsForZSet().incrementScore(sb.toString(), keyword, 1);
			sb.append(":");
			sb.append(day);
			pageViewStringRedisTemplate.opsForZSet().incrementScore(sb.toString(), keyword, 1);
		}
	}

	private void analyzeLocation(String day, String ip, String domain) {
		Location loc = LocationUtils.parse(ip);
		if (loc != null) {
			String province = loc.getFirstArea();
			if (StringUtils.isNotBlank(province)) {
				StringBuilder sb = new StringBuilder(KEY_PAGE_VIEW);
				if (StringUtils.isNotBlank(domain))
					sb.append(domain).append(":");
				sb.append("loc:pr");
				pageViewStringRedisTemplate.opsForZSet().incrementScore(sb.toString(), province, 1);
				sb.append(":");
				sb.append(day);
				pageViewStringRedisTemplate.opsForZSet().incrementScore(sb.toString(), province, 1);
			}
			String city = loc.getSecondArea();
			if (StringUtils.isNotBlank(city)) {
				StringBuilder sb = new StringBuilder(KEY_PAGE_VIEW);
				if (StringUtils.isNotBlank(domain))
					sb.append(domain).append(":");
				sb.append("loc:ct");
				pageViewStringRedisTemplate.opsForZSet().incrementScore(sb.toString(), city, 1);
				sb.append(":");
				sb.append(day);
				pageViewStringRedisTemplate.opsForZSet().incrementScore(sb.toString(), city, 1);
			}
		}
	}

	@Override
	public Set<String> getDomains() {
		if (pageViewStringRedisTemplate == null)
			return Collections.emptySet();
		return pageViewStringRedisTemplate.opsForSet().members(KEY_PAGE_VIEW + "domains");
	}

	@Override
	public long getPageView(String key, String domain) {
		return get(key, "pv", domain);
	}

	@Override
	public long getUniqueIp(String key, String domain) {
		return get(key, "uip", domain);
	}

	@Override
	public long getUniqueSessionId(String key, String domain) {
		return get(key, "usid", domain);
	}

	@Override
	public long getUniqueUsername(String key, String domain) {
		return get(key, "uu", domain);
	}

	@Override
	public Tuple<String, Long> getMaxPageView(String domain) {
		return getMax("pv", domain);
	}

	@Override
	public Tuple<String, Long> getMaxUniqueIp(String domain) {
		return getMax("uip", domain);
	}

	@Override
	public Tuple<String, Long> getMaxUniqueSessionId(String domain) {
		return getMax("usid", domain);
	}

	@Override
	public Tuple<String, Long> getMaxUniqueUsername(String domain) {
		return getMax("uu", domain);
	}

	@Override
	public Map<String, Long> getTopPageViewUrls(String day, int top, String domain) {
		return getTop(day, "url", top, domain);
	}

	@Override
	public Map<String, Long> getTopForeignReferers(String day, int top, String domain) {
		return getTop(day, "fr", top, domain);
	}

	@Override
	public Map<String, Long> getTopKeywords(String day, int top, String domain) {
		return getTop(day, "kw", top, domain);
	}

	@Override
	public Map<String, Long> getTopSearchEngines(String day, int top, String domain) {
		return getTop(day, "se", top, domain);
	}

	@Override
	public Map<String, Long> getTopProvinces(String day, int top, String domain) {
		return getTop(day, "loc:pr", top, domain);
	}

	@Override
	public Map<String, Long> getTopCities(String day, int top, String domain) {
		return getTop(day, "loc:ct", top, domain);
	}

	@Trigger
	@Mutex
	@Scheduled(cron = "${pageViewService.archive.cron:0 5 0 * * ?}")
	public void archive() {
		if (pageViewStringRedisTemplate == null)
			return;
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_YEAR, -1);
		Date yesterday = cal.getTime();
		String day = DateUtils.formatDate8(yesterday);
		pageViewStringRedisTemplate.delete(Arrays.asList(KEY_PAGE_VIEW + "uip:" + day + KEY_HYPERLOGLOG_SUFFIX,
				KEY_PAGE_VIEW + "usid:" + day + KEY_HYPERLOGLOG_SUFFIX,
				KEY_PAGE_VIEW + "uu:" + day + KEY_HYPERLOGLOG_SUFFIX));
		updateMax(day, "pv", null);
		updateMax(day, "uip", null);
		updateMax(day, "usid", null);
		updateMax(day, "uu", null);
		for (String domain : getDomains()) {
			pageViewStringRedisTemplate
					.delete(Arrays.asList(KEY_PAGE_VIEW + domain + ":" + "uip:" + day + KEY_HYPERLOGLOG_SUFFIX,
							KEY_PAGE_VIEW + domain + ":" + "usid:" + day + KEY_HYPERLOGLOG_SUFFIX,
							KEY_PAGE_VIEW + domain + ":" + "uu:" + day + KEY_HYPERLOGLOG_SUFFIX));
			updateMax(day, "pv", domain);
			updateMax(day, "uip", domain);
			updateMax(day, "usid", domain);
			updateMax(day, "uu", domain);
		}
	}

	private boolean addUnique(String day, String type, final String value, String domain) {
		if (StringUtils.isBlank(value))
			return false;
		StringBuilder sb = new StringBuilder(KEY_PAGE_VIEW);
		if (StringUtils.isNotBlank(domain))
			sb.append(domain).append(":");
		sb.append(type).append(":").append(day);
		String key = sb.toString();
		sb.append(KEY_HYPERLOGLOG_SUFFIX);
		HyperLogLogOperations<String, String> hll = pageViewStringRedisTemplate.opsForHyperLogLog();
		Long result = hll.add(sb.toString(), value);
		if (result > 0) {
			pageViewStringRedisTemplate.opsForValue().increment(key, 1);
			return true;
		}
		return false;
	}

	private long get(String key, String type, String domain) {
		if (pageViewStringRedisTemplate == null)
			return 0;
		StringBuilder sb = new StringBuilder(KEY_PAGE_VIEW);
		if (StringUtils.isNotBlank(domain))
			sb.append(domain).append(":");
		sb.append(type);
		if (key != null) {
			sb.append(":").append(key);
			String prefix = sb.toString();
			String value = pageViewStringRedisTemplate.opsForValue().get(prefix);
			if (value != null) {
				return Long.valueOf(value);
			} else {
				Set<String> keys = pageViewStringRedisTemplate.<Set<String>>execute((RedisConnection conn) -> {
					Set<String> set = new HashSet<>();
					Cursor<byte[]> cursor = conn
							.scan(ScanOptions.scanOptions().match(prefix + "*").count(100).build());
					while (cursor.hasNext())
						set.add((String) pageViewStringRedisTemplate.getKeySerializer().deserialize(cursor.next()));
					return set;
				});
				if (keys == null)
					throw new RuntimeException("Unexpected null");
				List<String> results = pageViewStringRedisTemplate.opsForValue().multiGet(keys);
				if (results == null)
					throw new RuntimeException("Unexpected null");
				long count = 0;
				for (String str : results)
					count += Long.valueOf(str);
				return count;
			}
		} else {
			String value = pageViewStringRedisTemplate.opsForValue().get(sb.toString());
			if (value != null)
				return Long.valueOf(value);
			return 0;
		}
	}

	private Tuple<String, Long> getMax(String type, String domain) {
		if (pageViewStringRedisTemplate == null)
			return null;
		StringBuilder sb = new StringBuilder(KEY_PAGE_VIEW);
		if (StringUtils.isNotBlank(domain))
			sb.append(domain).append(":");
		sb.append("max");
		String str = (String) pageViewStringRedisTemplate.opsForHash().get(sb.toString(), type);
		if (StringUtils.isNotBlank(str)) {
			String[] arr = str.split(",");
			return Tuple.of(arr[0], Long.valueOf(arr[1]));
		}
		return null;
	}

	private void updateMax(String day, String type, String domain) {
		if (pageViewStringRedisTemplate == null)
			return;
		long value = get(day, type, null);
		long oldvalue = 0;
		StringBuilder sb = new StringBuilder(KEY_PAGE_VIEW);
		if (StringUtils.isNotBlank(domain))
			sb.append(domain).append(":");
		sb.append("max");
		String key = sb.toString();
		String str = (String) pageViewStringRedisTemplate.opsForHash().get(key, type);
		if (StringUtils.isNotBlank(str))
			oldvalue = Long.valueOf(str.split(",")[1]);
		if (value > oldvalue)
			pageViewStringRedisTemplate.opsForHash().put(key, type, day + ',' + value);
	}

	public Map<String, Long> getTop(String day, String type, int top, String domain) {
		if (pageViewStringRedisTemplate == null)
			return Collections.emptyMap();
		StringBuilder sb = new StringBuilder(KEY_PAGE_VIEW);
		if (StringUtils.isNotBlank(domain))
			sb.append(domain).append(":");
		sb.append(type);
		if (day != null)
			sb.append(":").append(day);
		String key = sb.toString();
		Set<String> set = pageViewStringRedisTemplate.opsForZSet().reverseRange(key, 0, top - 1);
		if (set == null)
			throw new RuntimeException("Unexpected null");
		Map<String, Long> map = new LinkedHashMap<>();
		for (String member : set) {
			Double score = pageViewStringRedisTemplate.opsForZSet().score(key, member);
			if (score == null)
				throw new RuntimeException("Unexpected null");
			map.put(member, score.longValue());
		}
		return map;
	}

	private static String[] parseSearchUrl(String searchUrl) {
		URI url = URI.create(searchUrl);
		String host = url.getHost();
		String query = url.getQuery();
		for (Map.Entry<String, String> entry : searchengines.entrySet()) {
			if (host.indexOf(entry.getKey() + ".") == 0 || host.indexOf("." + entry.getKey() + ".") > 0) {
				String[] result = new String[2];
				result[0] = entry.getKey();
				result[1] = RequestUtils.getValueFromQueryString(query, entry.getValue());
				return result;
			}
		}
		return null;
	}

	private static Map<String, String> searchengines = new HashMap<String, String>() {
		private static final long serialVersionUID = 1L;

		{
			put("google", "q");
			put("bing", "q");
			put("taobao", "q");
			put("360", "q");
			put("so", "q");
			put("yahoo", "p");
			put("baidu", "wd");
			put("sogou", "query");
			put("soso", "w");
			put("youdao", "q");
		}
	};

}
