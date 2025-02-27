package org.ironrhino.core.util;

import java.util.Random;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Snowflake {

	public static final Snowflake DEFAULT_INSTANCE;

	private final static long EPOCH = 1556150400000L;
	private final static Random RANDOM = new Random();
	private final int workerId;
	private final int workerIdBits;
	private final int sequenceBits;
	private final long sequenceMask;
	private long sequence = 0L;
	private long lastTimestamp = -1L;

	static {
		int workerId = 0;
		int workerIdBits = 8;
		int sequenceBits = 10;
		String id = AppInfo.getEnv("worker.id");
		if (id == null) {
			if (AppInfo.isRunInKubernetes()) {
				String hostName = AppInfo.getHostName();
				if (hostName.matches(".+-\\d+$")) {
					// Kubernetes StatefulSet
					id = hostName.substring(hostName.lastIndexOf('-') + 1);
				}
			}
			if (id == null) {
				String ip = AppInfo.getHostAddress();
				int index = ip.lastIndexOf('.');
				if (index > 0) {
					id = ip.substring(index + 1);
				} else {
					// IPv6
					index = ip.lastIndexOf(':');
					id = ip.substring(index + 1);
					id = String.valueOf(NumberUtils.xToDecimal(16, id.toUpperCase()));
					workerIdBits = 16;
				}
			}
		}
		if (id != null) {
			try {
				workerId = Integer.parseInt(id);
				if (workerId < 0)
					workerId += 2 << workerIdBits;
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Snowflake worker id should be an integer", e);
			}
			String offset = AppInfo.getEnv("worker.id.offset");
			// workaround, ".spec.ordinals.start" is not supported by k8s lower than v1.31
			if (offset != null) {
				try {
					workerId += Integer.parseInt(offset);
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("Snowflake worker id offset should be an integer", e);
				}
			}
		}
		log.info("Snowflake default instance worker id is {}", workerId);
		DEFAULT_INSTANCE = new Snowflake(workerId, workerIdBits, sequenceBits);
	}

	public Snowflake(int workerId) {
		this(workerId, 8, 10);
	}

	public Snowflake(int workerId, int workerIdBits, int sequenceBits) {
		long maxWorkerId = -1L ^ -1L << workerIdBits;
		if (workerId > maxWorkerId || workerId < 0) {
			throw new IllegalArgumentException(
					String.format("workerId can't be greater than %d or less than 0", maxWorkerId));
		}
		this.workerId = workerId;
		this.workerIdBits = workerIdBits;
		this.sequenceBits = sequenceBits;
		this.sequenceMask = -1L ^ -1L << sequenceBits;
	}

	public synchronized long nextId() {
		long timestamp = System.currentTimeMillis();
		if (timestamp == lastTimestamp) {
			sequence = (sequence + 1) & sequenceMask;
			if (sequence == 0) {
				timestamp = System.currentTimeMillis();
				while (timestamp <= lastTimestamp) {
					timestamp = System.currentTimeMillis();
				}
			}
		} else if (timestamp > lastTimestamp) {
			sequence = RANDOM.nextInt(2);
		} else {
			long offset = lastTimestamp - timestamp;
			if (offset < 5000) {
				try {
					Thread.sleep(offset + 1);
					timestamp = System.currentTimeMillis();
					sequence = RANDOM.nextInt(2);
				} catch (InterruptedException e) {
					throw new IllegalStateException(e);
				}
			} else {
				throw new IllegalStateException(
						String.format("Clock moved backwards. Refusing to generate id for %d milliseconds", offset));
			}
		}
		lastTimestamp = timestamp;
		return ((timestamp - EPOCH) << (sequenceBits + workerIdBits)) | (workerId << sequenceBits) | sequence;
	}

	public Info parse(long id) {
		return new Info(id, workerIdBits, sequenceBits);
	}

	@Value
	public static class Info {
		private long timestamp;
		private int workerId;
		private long sequence;

		Info(long id, int workerIdBits, int sequenceBits) {
			long duration = id >> (sequenceBits + workerIdBits);
			timestamp = EPOCH + duration;
			workerId = (int) ((id - (duration << (sequenceBits + workerIdBits))) >> (sequenceBits));
			sequence = id - (duration << (sequenceBits + workerIdBits)) - (workerId << sequenceBits);
		}
	}

}
