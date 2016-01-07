package org.ironrhino.core.remoting.impl;

import static org.ironrhino.core.metadata.Profiles.DEFAULT;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ironrhino.common.model.tuples.Pair;
import org.ironrhino.core.remoting.ServiceStats;
import org.ironrhino.core.remoting.StatsType;
import org.ironrhino.core.spring.configuration.ServiceImplementationConditional;
import org.springframework.stereotype.Component;

@Component("serviceStats")
@ServiceImplementationConditional(profiles = DEFAULT)
public class StandaloneServiceStats implements ServiceStats {

	@Override
	public void emit(String serviceName, String method, long time, StatsType type) {

	}

	@Override
	public Map<String, Set<String>> getServices() {
		return Collections.emptyMap();
	}

	@Override
	public long getCount(String service, String key, StatsType type) {
		return 0;
	}

	@Override
	public Pair<String, Long> getMaxCount(String service, StatsType type) {
		return null;
	}

	@Override
	public List<String> findHotspots(int limit) {
		return Collections.emptyList();
	}

}