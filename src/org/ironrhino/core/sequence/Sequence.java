package org.ironrhino.core.sequence;

public interface Sequence {

	String DEFAULT_TABLE_NAME = "common_sequence";

	int nextIntValue();

	long nextLongValue();

	String nextStringValue();

}