package org.ironrhino.core.fs;

import java.io.Serializable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Paged<T> implements Serializable {

	private static final long serialVersionUID = 5016790873200595151L;

	private final String marker;

	private final String nextMarker;

	private final List<T> result;

}
