package org.ironrhino.common.service;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.ironrhino.common.model.tuples.Pair;

public interface PageViewService {

	public void put(Date date, String ip, String url, String sessionId,
			String username, String referer);

	public Set<String> getDomains();

	public long getPageView(String key, String domain);

	public long getUniqueIp(String day, String domain);

	public long getUniqueSessionId(String day, String domain);

	public long getUniqueUsername(String day, String domain);

	public Pair<String, Long> getMaxPageView(String domain);

	public Pair<String, Long> getMaxUniqueIp(String domain);

	public Pair<String, Long> getMaxUniqueSessionId(String domain);

	public Pair<String, Long> getMaxUniqueUsername(String domain);

	public Map<String, Long> getTopPageViewUrls(String day, int top,
			String domain);

	public Map<String, Long> getTopForeignReferers(String day, int top,
			String domain);

	public Map<String, Long> getTopKeywords(String day, int top, String domain);

	public Map<String, Long> getTopSearchEngines(String day, int top,
			String domain);

	public Map<String, Long> getTopProvinces(String day, int top, String domain);

	public Map<String, Long> getTopCities(String day, int top, String domain);

}
