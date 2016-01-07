package org.ironrhino.core.remoting;

import java.util.Map;
import java.util.Set;

import org.ironrhino.common.model.tuples.Pair;

public interface ServiceStats {

	public void emit(String serviceName, String method, long time, boolean failed, boolean clientSide);

	public Map<String, Set<String>> getServices();

	public Pair<String, Long> getMaxCount(String serviceName, String method, boolean clientSide);

	public long getCount(String serviceName, String method, String key, boolean clientSide);

}