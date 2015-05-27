package org.ironrhino.core.sequence;

public interface Sequence {

	public static final String DEFAULT_TABLE_NAME = "common_sequence";

	public int nextIntValue();

	public String nextStringValue();

}