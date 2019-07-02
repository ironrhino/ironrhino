package org.ironrhino.core.remoting.stats;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ironrhino.core.model.Tuple;

public interface ServiceStats {

	void serverSideEmit(String serviceName, String method, long time);

	void clientSideEmit(String target, String serviceName, String method, long time, boolean failed);

	Map<String, Set<String>> getServices();

	Tuple<String, Long> getMaxCount(String service, StatsType type);

	long getCount(String service, String key, StatsType type);

	Map<String, Long> findHotspots(int limit);

	List<InvocationWarning> getWarnings();

	List<InvocationSample> getSamples(String service, StatsType type);

}