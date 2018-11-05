package org.ironrhino.core.remoting;

import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@FunctionalInterface
public interface DistanceMeasurer {

	DistanceMeasurer DEFAULT = (from, to) -> {
		String[] arr1 = from.split("\\.");
		String[] arr2 = to.split("\\.");
		return Math.abs(Integer.valueOf(arr1[0]) - Integer.valueOf(arr2[0])) * 256 * 256
				+ Math.abs(Integer.valueOf(arr1[1]) - Integer.valueOf(arr2[1])) * 256
				+ Math.abs(Integer.valueOf(arr1[2]) - Integer.valueOf(arr2[2]));
	};

	Pattern pattern = Pattern.compile("((\\d+\\.){3}\\d+)");

	int distance(String fromIp, String toIp);

	default int measureDistance(String from, String to) {
		if (from == null || to == null)
			return Integer.MAX_VALUE;
		Matcher matcher = pattern.matcher(from);
		String fromIp = matcher.find() ? matcher.group() : null;
		matcher = pattern.matcher(to);
		String toIp = matcher.find() ? matcher.group() : null;
		if (fromIp == null || toIp == null)
			return Integer.MAX_VALUE;
		return distance(fromIp, toIp);
	}

	default List<String> findNearest(String origin, List<String> candidates) {
		if (candidates.size() < 2)
			return candidates;
		return candidates.stream()
				.collect(Collectors.groupingBy(s -> measureDistance(origin, s), TreeMap::new, Collectors.toList()))
				.entrySet().iterator().next().getValue();
	}

}
