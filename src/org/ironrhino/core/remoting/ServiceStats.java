package org.ironrhino.core.remoting;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ironrhino.common.model.tuples.Pair;

public interface ServiceStats {

	public void serverSideEmit(String serviceName, String method, long time);

	public void clientSideEmit(String source, String target, String serviceName, String method, long time, boolean failed);

	public Map<String, Set<String>> getServices();

	public Pair<String, Long> getMaxCount(String service, StatsType type);

	public long getCount(String service, String key, StatsType type);

	public List<String> findHotspots(int limit);

}