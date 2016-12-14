package org.ironrhino.core.jdbc;

import java.io.Serializable;

public class Limiting implements Serializable {

	private static final long serialVersionUID = 6261424874344528247L;

	private final int offset;

	private final int limit;

	private Limiting(int offset, int limit) {
		this.offset = offset;
		this.limit = limit;
	}

	public int getOffset() {
		return offset;
	}

	public int getLimit() {
		return limit;
	}

	public static Limiting of(int limit) {
		return new Limiting(0, limit);
	}

	public static Limiting of(int offset, int limit) {
		return new Limiting(offset, limit);
	}

}
