package org.ironrhino.core.jdbc;

import java.io.Serializable;

import lombok.Value;

@Value(staticConstructor = "of")
public class Limiting implements Serializable {

	private static final long serialVersionUID = 6261424874344528247L;

	int offset;

	int limit;

	public static Limiting of(int limit) {
		return of(0, limit);

	}

}
