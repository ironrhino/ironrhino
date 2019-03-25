package org.ironrhino.core.sequence;

public interface SimpleSequence extends Sequence {

	@Override
	public default int nextIntValue() {
		long value = nextLongValue();
		if (value > Integer.MAX_VALUE)
			throw new RuntimeException(value + " exceed Integer.MAX_VALUE");
		return (int) value;
	}

	public void restart();

}