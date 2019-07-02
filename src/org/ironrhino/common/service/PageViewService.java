package org.ironrhino.common.service;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.ironrhino.core.model.Tuple;

public interface PageViewService {

	void put(Date date, String ip, String url, String sessionId, String username, String referer);

	Set<String> getDomains();

	long getPageView(String key, String domain);

	long getUniqueIp(String day, String domain);

	long getUniqueSessionId(String day, String domain);

	long getUniqueUsername(String day, String domain);

	Tuple<String, Long> getMaxPageView(String domain);

	Tuple<String, Long> getMaxUniqueIp(String domain);

	Tuple<String, Long> getMaxUniqueSessionId(String domain);

	Tuple<String, Long> getMaxUniqueUsername(String domain);

	Map<String, Long> getTopPageViewUrls(String day, int top, String domain);

	Map<String, Long> getTopForeignReferers(String day, int top, String domain);

	Map<String, Long> getTopKeywords(String day, int top, String domain);

	Map<String, Long> getTopSearchEngines(String day, int top, String domain);

	Map<String, Long> getTopProvinces(String day, int top, String domain);

	Map<String, Long> getTopCities(String day, int top, String domain);

}
