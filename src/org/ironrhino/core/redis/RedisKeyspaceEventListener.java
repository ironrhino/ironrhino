package org.ironrhino.core.redis;

public interface RedisKeyspaceEventListener {

	void onEvent(String event, String key);

}