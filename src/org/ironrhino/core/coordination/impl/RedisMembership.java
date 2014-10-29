package org.ironrhino.core.coordination.impl;

import static org.ironrhino.core.metadata.Profiles.CLOUD;
import static org.ironrhino.core.metadata.Profiles.DUAL;

import java.util.List;

import org.ironrhino.core.coordination.Membership;
import org.ironrhino.core.spring.configuration.ResourcePresentConditional;
import org.ironrhino.core.util.AppInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component("membership")
@Profile({ DUAL, CLOUD })
@ResourcePresentConditional(value = "resources/spring/applicationContext-coordination.xml", negated = true)
public class RedisMembership implements Membership {

	private static final String NAMESPACE = "membership:";

	@Autowired
	@Qualifier("stringRedisTemplate")
	private RedisTemplate<String, String> stringRedisTemplate;

	@Override
	public void join(final String group) throws Exception {
		stringRedisTemplate.opsForList().rightPush(NAMESPACE + group,
				AppInfo.getInstanceId());
	}

	@Override
	public void leave(final String group) throws Exception {
		stringRedisTemplate.opsForList().remove(NAMESPACE + group, 0,
				AppInfo.getInstanceId());
	}

	@Override
	public boolean isLeader(String group) throws Exception {
		return AppInfo.getInstanceId().equals(getLeader(group));
	}

	@Override
	public String getLeader(String group) throws Exception {
		List<String> list = getMembers(group);
		if (!list.isEmpty())
			return list.get(0);
		return null;
	}

	@Override
	public List<String> getMembers(String group) throws Exception {
		return stringRedisTemplate.opsForList().range(NAMESPACE + group, 0, -1);
	}

}
