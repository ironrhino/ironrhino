package org.ironrhino.core.remoting.impl;

import static org.ironrhino.core.metadata.Profiles.DEFAULT;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.ironrhino.common.model.tuples.Pair;
import org.ironrhino.core.remoting.ServiceStats;
import org.ironrhino.core.spring.configuration.ServiceImplementationConditional;
import org.springframework.stereotype.Component;

@Component("serviceStats")
@ServiceImplementationConditional(profiles = DEFAULT)
public class StandaloneServiceStats implements ServiceStats {

	@Override
	public void emit(String serviceName, String method, long time, boolean failed, boolean clientSide) {

	}

	@Override
	public Map<String, Set<String>> getServices() {
		return Collections.emptyMap();
	}

	@Override
	public long getCount(String serviceName, String method, String key, boolean clientSide) {
		return 0;
	}
	
	@Override
	public Pair<String, Long> getMaxCount(String serviceName, String method, boolean clientSide){
		return null;
	}

}