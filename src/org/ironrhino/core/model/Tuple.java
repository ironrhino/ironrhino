package org.ironrhino.core.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tuple<K, V> implements Serializable {

	private static final long serialVersionUID = 3468521016262233197L;

	private K key;

	private V value;

}
