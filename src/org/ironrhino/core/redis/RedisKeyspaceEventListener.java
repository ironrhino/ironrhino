package org.ironrhino.core.redis;

@FunctionalInterface
public interface RedisKeyspaceEventListener {

	void onEvent(String event, String key);

}