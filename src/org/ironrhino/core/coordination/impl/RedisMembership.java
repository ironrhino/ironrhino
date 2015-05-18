package org.ironrhino.core.coordination.impl;

import static org.ironrhino.core.metadata.Profiles.CLOUD;
import static org.ironrhino.core.metadata.Profiles.DUAL;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.ironrhino.core.coordination.Membership;
import org.ironrhino.core.spring.configuration.ResourcePresentConditional;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.HttpClientUtils;
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

	private Set<String> groups = Collections
			.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

	@Autowired
	private TaskScheduler taskScheduler;

	@Value("${redis.membership.heartbeat:60000}")
	private int heartbeat = 60000;

	@Autowired
	public RedisMembership(
			@Qualifier("stringRedisTemplate") RedisTemplate<String, String> stringRedisTemplate) {
		this.stringRedisTemplate = stringRedisTemplate;
	}

	@PostConstruct
	public void afterPropertiesSet() {
		taskScheduler.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				for (String group : groups) {
					List<String> members = getMembers(group);
					String self = AppInfo.getInstanceId();
					if (!members.contains(self))
						stringRedisTemplate.opsForList().rightPush(
								NAMESPACE + group, self);
					for (String member : members) {
						if (member.equals(self))
							continue;
						boolean alive = true;
						StringBuilder sb = new StringBuilder("http://");
						sb.append(member.substring(member.lastIndexOf('@') + 1));
						sb.append("/_ping");
						try {
							String value = HttpClientUtils.getResponseText(
									sb.toString()).trim();
							if (!value.equals(member)) {
								alive = false;
								if (!members.contains(value))
									stringRedisTemplate.opsForList().rightPush(
											NAMESPACE + group, value);
							}
						} catch (IOException e) {
							alive = false;
						}
						if (!alive)
							stringRedisTemplate.opsForList().remove(
									NAMESPACE + group, 0, member);
					}
				}
			}
		}, heartbeat);
	}

	@Override
	public void join(final String group) {
		stringRedisTemplate.opsForList().leftPush(NAMESPACE + group,
				AppInfo.getInstanceId());
		groups.add(group);
	}

	@Override
	public void leave(final String group) {
		stringRedisTemplate.opsForList().remove(NAMESPACE + group, 0,
				AppInfo.getInstanceId());
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
