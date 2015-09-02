package org.ironrhino.core.coordination.impl;

import static org.ironrhino.core.metadata.Profiles.CLOUD;
import static org.ironrhino.core.metadata.Profiles.DUAL;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.ironrhino.core.coordination.Membership;
import org.ironrhino.core.spring.configuration.ResourcePresentConditional;
import org.ironrhino.core.util.AppInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

@Component("membership")
@Profile({ DUAL, CLOUD })
@ResourcePresentConditional(value = "resources/spring/applicationContext-coordination.xml", negated = true)
public class RedisMembership implements Membership {

	private static final String NAMESPACE = "membership:";

	private RedisTemplate<String, String> stringRedisTemplate;

	private Set<String> groups = Collections.newSetFromMap(new ConcurrentHashMap<>());

	@Autowired
	private TaskScheduler taskScheduler;

	@Value("${redis.membership.heartbeat:60000}")
	private int heartbeat = 60000;

	@Autowired
	public RedisMembership(@Qualifier("stringRedisTemplate") RedisTemplate<String, String> stringRedisTemplate) {
		this.stringRedisTemplate = stringRedisTemplate;
	}

	@PostConstruct
	public void afterPropertiesSet() {
		taskScheduler.scheduleAtFixedRate(() -> {
			for (String group : groups) {
				List<String> members = getMembers(group);
				String self = AppInfo.getInstanceId();
				if (!members.contains(self))
					stringRedisTemplate.opsForList().rightPush(NAMESPACE + group, self);
				for (String member : members) {
					if (member.equals(self))
						continue;
					boolean alive = false;
					String url = new StringBuilder("http://").append(member.substring(member.lastIndexOf('@') + 1))
							.append("/_ping?_internal_testing_").toString();
					try {
						HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
						conn.setConnectTimeout(3000);
						conn.setReadTimeout(2000);
						conn.setInstanceFollowRedirects(false);
						conn.setDoOutput(false);
						conn.setUseCaches(false);
						conn.connect();
						if (conn.getResponseCode() == 200) {
							InputStream is = conn.getInputStream();
							List<String> lines = IOUtils.readLines(is);
							is.close();
							if (lines.size() > 0) {
								String value = lines.get(0).trim();
								if (value.equals(member)) {
									alive = true;
								} else {
									if (!members.contains(value) && value.length() <= 100
											&& value.matches("[\\w-]+@[\\w.:]+")) {
										if (AppInfo.getAppName().equals(value.substring(0, value.lastIndexOf('-')))) {
											stringRedisTemplate.opsForList().rightPush(NAMESPACE + group, value);
										} else {
											// multiple virtual host
											alive = true;
										}
									}
								}
							}
						}
						conn.disconnect();
					} catch (IOException e) {
					}
					if (!alive)
						stringRedisTemplate.opsForList().remove(NAMESPACE + group, 0, member);
				}
			}
		} , heartbeat);
	}

	@Override
	public void join(final String group) {
		stringRedisTemplate.opsForList().leftPush(NAMESPACE + group, AppInfo.getInstanceId());
		groups.add(group);
	}

	@Override
	public void leave(final String group) {
		stringRedisTemplate.opsForList().remove(NAMESPACE + group, 0, AppInfo.getInstanceId());
		groups.remove(group);
	}

	@Override
	public boolean isLeader(String group) {
		return AppInfo.getInstanceId().equals(getLeader(group));
	}

	@Override
	public String getLeader(String group) {
		List<String> list = getMembers(group);
		if (!list.isEmpty())
			return list.get(0);
		return null;
	}

	@Override
	public List<String> getMembers(String group) {
		return stringRedisTemplate.opsForList().range(NAMESPACE + group, 0, -1);
	}

}
