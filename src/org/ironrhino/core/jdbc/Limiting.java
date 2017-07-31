package org.ironrhino.core.jdbc;

import java.io.Serializable;

import lombok.Data;

@Data(staticConstructor = "of")
public class Limiting implements Serializable {

	private static final long serialVersionUID = 6261424874344528247L;

	private final int offset;

	private final int limit;

	public static Limiting of(int limit) {
		return of(0, limit);
	}

}
