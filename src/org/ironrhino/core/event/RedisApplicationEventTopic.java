package org.ironrhino.core.event;

import static org.ironrhino.core.metadata.Profiles.CLOUD;
import static org.ironrhino.core.metadata.Profiles.CLUSTER;
import static org.ironrhino.core.metadata.Profiles.DUAL;

import org.ironrhino.core.redis.RedisTopic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile({ DUAL, CLUSTER, CLOUD, "redis" })
@Component
public class RedisApplicationEventTopic extends RedisTopic<ApplicationEvent> implements ApplicationEventTopic {

	@Autowired
	private ApplicationContext ctx;

	@Override
	public void subscribe(ApplicationEvent event) {
		ctx.publishEvent(event);
	}

}
