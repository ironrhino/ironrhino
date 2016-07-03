package org.ironrhino.core.stat;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.ironrhino.core.util.DateUtils;

public class StatLog {

	private static final Lock timerLock = new ReentrantLock();

	private static final Condition condition = timerLock.newCondition();

	private static final ConcurrentHashMap<Key, Value> data = new ConcurrentHashMap<>(64);

	private static Thread writeThread;

	static {
		startNewThread();
		Runtime.getRuntime().addShutdownHook(new Thread() {

			@Override
			public void run() {
				write(false);
			}
		});
	}

	private static void runWriteThread() {
		if (writeThread.isAlive())
			return;
		try {
			writeThread.interrupt();
		} catch (Exception e) {
			e.printStackTrace();
		}
		startNewThread();
	}

	private static void startNewThread() {
		writeThread = new Thread(() -> {
			while (true) {
				timerLock.lock();
				try {
					condition.await(StatLogSettings.getIntervalUnit(), TimeUnit.SECONDS);
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					timerLock.unlock();
				}
				StatLog.write();
			}
		}, StatLogSettings.WRITETHREAD_NAME);
		writeThread.setDaemon(true);
		writeThread.start();
	}

	private static void write() {
		write(true);
	}

	private static void write(boolean checkInterval) {
		Map<Key, Value> temp = new HashMap<>();
		for (Map.Entry<Key, Value> entry : data.entrySet()) {
			long current = System.currentTimeMillis();
			Key key = entry.getKey();
			Value value = entry.getValue();
			if (((!checkInterval || (current - key.getLastWriteTime()) / StatLogSettings.getIntervalUnit() > key
					.getIntervalMultiple())) && (value.getLongValue() > 0 || value.getDoubleValue() > 0)) {
				key.setLastWriteTime(current);
				output(key, value);
				temp.put(key, new Value(value.getLongValue(), value.getDoubleValue()));
			}
		}
		for (Map.Entry<Key, Value> entry : temp.entrySet()) {
			Key key = entry.getKey();
			Value value = data.get(key);
			value.add(-entry.getValue().getLongValue(), -entry.getValue().getDoubleValue());
		}
	}

	private synchronized static void output(Key key, Value value) {
		StringBuilder sb = new StringBuilder();
		sb.append(key);
		sb.append(StatLogSettings.TOKEN);
		sb.append(value);
		sb.append(StatLogSettings.TOKEN);
		sb.append(sysdate());
		sb.append('\n');
		try {
			FileOutputStream fileOutputStream = new FileOutputStream(StatLogSettings.getLogFile(
					StatLogSettings.STAT_LOG_FILE_NAME + DateUtils.format(new Date(), StatLogSettings.DATE_STYLE)),
					true);
			fileOutputStream.write(sb.toString().getBytes(StatLogSettings.ENCODING));
			fileOutputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static String sysdate() {
		return String.valueOf(System.currentTimeMillis());
	}

	private static Value getValue(Key key) {
		return data.computeIfAbsent(key, k -> new Value(0));
	}

	public static void add(Key key, long c, double d) {
		if (!writeThread.isAlive())
			runWriteThread();
		Value value = getValue(key);
		value.add(c, d);
	}

	public static void add(Key key, long c) {
		add(key, c, 0);
	}

	public static void add(Key key, double d) {
		add(key, 0, d);
	}

	public static void add(String... names) {
		add(new Key(names), 1);
	}

	public static void add(Key key) {
		add(key, 1);
	}

}
