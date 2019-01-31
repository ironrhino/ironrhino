package org.ironrhino.core.sequence;

public interface Sequence {

	public static final String DEFAULT_TABLE_NAME = "common_sequence";

	public default int nextIntValue() {
		long value = nextLongValue();
		if (value > Integer.MAX_VALUE)
			throw new RuntimeException(value + " exceed Integer.MAX_VALUE");
		return (int) value;
	}

	public long nextLongValue();

	public String nextStringValue();

}