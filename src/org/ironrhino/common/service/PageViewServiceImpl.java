package org.ironrhino.common.service;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.common.model.tuples.Pair;
import org.ironrhino.common.util.Location;
import org.ironrhino.common.util.LocationUtils;
import org.ironrhino.core.metadata.Trigger;
import org.ironrhino.core.util.DateUtils;
import org.ironrhino.core.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PageViewServiceImpl implements PageViewService {

	public static final String KEY_PAGE_VIEW = "{pv}:";

	@Autowired(required = false)
	@Qualifier("stringRedisTemplate")
	private RedisTemplate<String, String> stringRedisTemplate;

	@Override
	public void put(Date date, String ip, String url, String sessionId,
			String username, String referer) {
		if (stringRedisTemplate == null)
			return;
		String domain = null;
		try {
			domain = new URL(url).getHost();
			if (domain.startsWith("www."))
				domain = domain.substring(4);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
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
		stringRedisTemplate.opsForSet().add(KEY_PAGE_VIEW + "domains", domain);
	}

	private void addPageView(Date date, String domain) {
		StringBuilder sb = new StringBuilder(KEY_PAGE_VIEW);
		if (StringUtils.isNotBlank(domain))
			sb.append(domain).append(":");
		sb.append("pv");
		stringRedisTemplate.opsForValue().increment(sb.toString(), 1);
		sb.append(":");
		String key = DateUtils.format(date, "yyyyMMddHH");
		stringRedisTemplate.opsForValue().increment(sb.toString() + key, 1);
		key = DateUtils.formatDate8(date);
		stringRedisTemplate.opsForValue().increment(sb.toString() + key, 1);
		key = DateUtils.format(date, "yyyyMM");
		stringRedisTemplate.opsForValue().increment(sb.toString() + key, 1);
		key = DateUtils.format(date, "yyyy");
		stringRedisTemplate.opsForValue().increment(sb.toString() + key, 1);
	}

	private void addUrlVisit(String day, String url, String domain) {
		StringBuilder sb = new StringBuilder(KEY_PAGE_VIEW);
		if (StringUtils.isNotBlank(domain))
			sb.append(domain).append(":");
		sb.append("url");
		stringRedisTemplate.opsForZSet().incrementScore(sb.toString(), url, 1);
		sb.append(":");
		sb.append(day);
		stringRedisTemplate.opsForZSet().incrementScore(sb.toString(), url, 1);
	}

	private void analyzeReferer(String day, String url, String referer,
			String domain) {
		if (StringUtils.isBlank(referer)
				|| RequestUtils.isSameOrigin(url, referer))
			return;

		StringBuilder sb = new StringBuilder(KEY_PAGE_VIEW);
		if (StringUtils.isNotBlank(domain))
			sb.append(domain).append(":");
		sb.append("fr");
		stringRedisTemplate.opsForZSet().incrementScore(sb.toString(), referer,
				1);
		sb.append(":");
		sb.append(day);
		stringRedisTemplate.opsForZSet().incrementScore(sb.toString(), referer,
				1);

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
			stringRedisTemplate.opsForZSet().incrementScore(sb.toString(),
					searchengine, 1);
			sb.append(":");
			sb.append(day);
			stringRedisTemplate.opsForZSet().incrementScore(sb.toString(),
					searchengine, 1);
		}
		if (StringUtils.isNotBlank(keyword)) {
			sb = new StringBuilder(KEY_PAGE_VIEW);
			if (StringUtils.isNotBlank(domain))
				sb.append(domain).append(":");
			sb.append("kw");
			stringRedisTemplate.opsForZSet().incrementScore(sb.toString(),
					keyword, 1);
			sb.append(":");
			sb.append(day);
			stringRedisTemplate.opsForZSet().incrementScore(sb.toString(),
					keyword, 1);
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
				stringRedisTemplate.opsForZSet().incrementScore(sb.toString(),
						province, 1);
				sb.append(":");
				sb.append(day);
				stringRedisTemplate.opsForZSet().incrementScore(sb.toString(),
						province, 1);
			}
			String city = loc.getSecondArea();
			if (StringUtils.isNotBlank(city)) {
				StringBuilder sb = new StringBuilder(KEY_PAGE_VIEW);
				if (StringUtils.isNotBlank(domain))
					sb.append(domain).append(":");
				sb.append("loc:ct");
				stringRedisTemplate.opsForZSet().incrementScore(sb.toString(),
						city, 1);
				sb.append(":");
				sb.append(day);
				stringRedisTemplate.opsForZSet().incrementScore(sb.toString(),
						city, 1);
			}
		}
	}

	@Override
	public Set<String> getDomains() {
		if (stringRedisTemplate == null)
			return Collections.emptySet();
		return stringRedisTemplate.opsForSet().members(
				KEY_PAGE_VIEW + "domains");
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
	public Pair<String, Long> getMaxPageView(String domain) {
		return getMax("pv", domain);
	}

	@Override
	public Pair<String, Long> getMaxUniqueIp(String domain) {
		return getMax("uip", domain);
	}

	@Override
	public Pair<String, Long> getMaxUniqueSessionId(String domain) {
		return getMax("usid", domain);
	}

	@Override
	public Pair<String, Long> getMaxUniqueUsername(String domain) {
		return getMax("uu", domain);
	}

	@Override
	public Map<String, Long> getTopPageViewUrls(String day, int top,
			String domain) {
		return getTop(day, "url", top, domain);
	}

	@Override
	public Map<String, Long> getTopForeignReferers(String day, int top,
			String domain) {
		return getTop(day, "fr", top, domain);
	}

	@Override
	public Map<String, Long> getTopKeywords(String day, int top, String domain) {
		return getTop(day, "kw", top, domain);
	}

	@Override
	public Map<String, Long> getTopSearchEngines(String day, int top,
			String domain) {
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
	@Scheduled(cron = "0 5 0 * * ?")
	public void archive() {
		if (stringRedisTemplate == null)
			return;
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_YEAR, -1);
		Date yesterday = cal.getTime();
		String day = DateUtils.formatDate8(yesterday);
		stringRedisTemplate.delete(new StringBuilder(KEY_PAGE_VIEW)
				.append("uip:").append(day).append("_set").toString());
		stringRedisTemplate.delete(new StringBuilder(KEY_PAGE_VIEW)
				.append("usid:").append(day).append("_set").toString());
		stringRedisTemplate.delete(new StringBuilder(KEY_PAGE_VIEW)
				.append("uu:").append(day).append("_set").toString());
		updateMax(day, "pv", null);
		updateMax(day, "uip", null);
		updateMax(day, "usid", null);
		updateMax(day, "uu", null);
		for (String domain : getDomains()) {
			stringRedisTemplate.delete(new StringBuilder(KEY_PAGE_VIEW)
					.append(domain).append(":").append("uip:").append(day)
					.append("_set").toString());
			stringRedisTemplate.delete(new StringBuilder(KEY_PAGE_VIEW)
					.append(domain).append(":").append("usid:").append(day)
					.append("_set").toString());
			stringRedisTemplate.delete(new StringBuilder(KEY_PAGE_VIEW)
					.append(domain).append(":").append("uu:").append(day)
					.append("_set").toString());
			updateMax(day, "pv", domain);
			updateMax(day, "uip", domain);
			updateMax(day, "usid", domain);
			updateMax(day, "uu", domain);
		}
	}

	private boolean addUnique(String day, String type, String value,
			String domain) {
		if (StringUtils.isBlank(value))
			return false;
		StringBuilder sb = new StringBuilder(KEY_PAGE_VIEW);
		if (StringUtils.isNotBlank(domain))
			sb.append(domain).append(":");
		sb.append(type).append(":").append(day);
		String key = sb.toString();
		sb.append("_set");
		if (stringRedisTemplate.opsForSet().add(sb.toString(), value) == 1) {
			stringRedisTemplate.opsForValue().increment(key, 1);
			return true;
		}
		return false;
	}

	private long get(String key, String type, String domain) {
		if (stringRedisTemplate == null)
			return 0;
		StringBuilder sb = new StringBuilder(KEY_PAGE_VIEW);
		if (StringUtils.isNotBlank(domain))
			sb.append(domain).append(":");
		sb.append(type);
		if (key != null)
			sb.append(":").append(key);
		String value = stringRedisTemplate.opsForValue().get(sb.toString());
		if (value != null)
			return Long.valueOf(value);
		return 0;
	}

	private Pair<String, Long> getMax(String type, String domain) {
		if (stringRedisTemplate == null)
			return null;
		StringBuilder sb = new StringBuilder(KEY_PAGE_VIEW);
		if (StringUtils.isNotBlank(domain))
			sb.append(domain).append(":");
		sb.append("max");
		String str = (String) stringRedisTemplate.opsForHash().get(
				sb.toString(), type);
		if (StringUtils.isNotBlank(str)) {
			String[] arr = str.split(",");
			return new Pair<String, Long>(arr[0], Long.valueOf(arr[1]));
		}
		return null;
	}

	private void updateMax(String day, String type, String domain) {
		if (stringRedisTemplate == null)
			return;
		long value = get(day, type, null);
		long oldvalue = 0;
		StringBuilder sb = new StringBuilder(KEY_PAGE_VIEW);
		if (StringUtils.isNotBlank(domain))
			sb.append(domain).append(":");
		sb.append("max");
		String key = sb.toString();
		String str = (String) stringRedisTemplate.opsForHash().get(key, type);
		if (StringUtils.isNotBlank(str))
			oldvalue = Long.valueOf(str.split(",")[1]);
		if (value > oldvalue)
			stringRedisTemplate.opsForHash().put(key, type, day + "," + value);
	}

	public Map<String, Long> getTop(String day, String type, int top,
			String domain) {
		if (stringRedisTemplate == null)
			return Collections.emptyMap();
		StringBuilder sb = new StringBuilder(KEY_PAGE_VIEW);
		if (StringUtils.isNotBlank(domain))
			sb.append(domain).append(":");
		sb.append(type);
		if (day != null)
			sb.append(":").append(day);
		String key = sb.toString();
		Set<String> set = stringRedisTemplate.opsForZSet().reverseRange(key, 0,
				top - 1);
		Map<String, Long> map = new LinkedHashMap<String, Long>();
		for (String member : set)
			map.put(member, stringRedisTemplate.opsForZSet().score(key, member)
					.longValue());
		return map;
	}

	private static String[] parseSearchUrl(String searchUrl) {
		try {
			URL url = new URL(searchUrl);
			String host = url.getHost();
			String query = url.getQuery();
			for (Map.Entry<String, String> entry : searchengines.entrySet()) {
				if (host.indexOf(entry.getKey() + ".") == 0
						|| host.indexOf("." + entry.getKey() + ".") > 0) {
					String[] result = new String[2];
					result[0] = entry.getKey();
					result[1] = RequestUtils.getValueFromQueryString(query,
							entry.getValue());
					return result;
				}
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
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
