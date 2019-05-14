package org.ironrhino.core.util;

public class Snowflake {

	private final static long EPOCH = 1556150400000L;
	private final static long WORKER_ID_BITS = 8L;
	private final static long MAX_WORKER_ID = -1L ^ -1L << WORKER_ID_BITS;
	private final static long SEQUENCE_BITS = 10L;
	private final static long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
	private final static long SEQUENCE_MASK = -1L ^ -1L << SEQUENCE_BITS;

	private final int workerId;
	private long sequence = 0L;
	private long lastTimestamp = -1L;

	public Snowflake(int workerId) {
		if (workerId > MAX_WORKER_ID || workerId < 0) {
			throw new IllegalArgumentException(
					String.format("Worker Id can't be greater than %d or less than 0", MAX_WORKER_ID));
		}
		this.workerId = workerId;
	}

	public synchronized long nextId() {
		long timestamp = System.currentTimeMillis();
		if (timestamp == lastTimestamp) {
			sequence = (sequence + 1) & SEQUENCE_MASK;
			if (sequence == 0) {
				timestamp = System.currentTimeMillis();
				while (timestamp <= lastTimestamp) {
					timestamp = System.currentTimeMillis();
				}
			}
		} else if (timestamp > lastTimestamp) {
			sequence = 0;
		} else {
			throw new IllegalStateException(String.format(
					"Clock moved backwards. Refusing to generate id for %d milliseconds", lastTimestamp - timestamp));
		}
		lastTimestamp = timestamp;
		return ((timestamp - EPOCH) << TIMESTAMP_LEFT_SHIFT) | (workerId << SEQUENCE_BITS) | sequence;
	}

}
