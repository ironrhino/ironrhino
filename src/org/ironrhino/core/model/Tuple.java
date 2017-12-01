package org.ironrhino.core.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Tuple<K, V> implements Serializable {

	private static final long serialVersionUID = 3468521016262233197L;

	private final K key;

	private final V value;

	public static <K, V> Tuple<K, V> of(K key, V value) {
		return new Tuple<K, V>(key, value);
	}

}
