package org.ironrhino.core.fs;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class Paged<T> implements Serializable {

	private static final long serialVersionUID = 5016790873200595151L;

	String marker;

	String nextMarker;

	List<T> result;

	static <T> Paged<T> from(List<T> result, int limit, String marker, Function<T, String> markerMapper) {
		int start;
		if (marker == null) {
			start = 0;
		} else {
			start = -1;
			for (int i = 0; i < result.size(); i++) {
				if (markerMapper.apply(result.get(i)).equals(marker))
					start = i;
			}
			if (start == -1)
				return new Paged<>(marker, null, Collections.emptyList());
		}
		return new Paged<>(marker, start + limit < result.size() ? markerMapper.apply(result.get(start + limit)) : null,
				result.subList(start, Math.min(start + limit, result.size())));
	}

}
