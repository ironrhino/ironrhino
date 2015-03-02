package org.ironrhino.core.stat;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
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

	private static final ConcurrentHashMap<Key, Value> data = new ConcurrentHashMap<Key, Value>(
			64);

	private static Thread writeThread;

	private static Date lastDate;

	private static FileOutputStream fileOutputStream;

	private static BufferedWriter bufferedWriter;

	static {
		startNewThread();
		Runtime.getRuntime().addShutdownHook(new Thread() {

			@Override
			public void run() {
				write(false);
				try {
					if (bufferedWriter != null)
						bufferedWriter.close();
					if (fileOutputStream != null)
						fileOutputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
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
		writeThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					timerLock.lock();
					try {
						condition.await(StatLogSettings.getIntervalUnit(),
								TimeUnit.SECONDS);
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						timerLock.unlock();
					}
					StatLog.write();
				}
			}

		}, StatLogSettings.WRITETHREAD_NAME);
		writeThread.setDaemon(true);
		writeThread.start();
	}

	private static void write() {
		write(true);
	}

	private static void write(boolean checkInterval) {
		Map<Key, Value> temp = new HashMap<Key, Value>();
		for (Map.Entry<Key, Value> entry : data.entrySet()) {
			long current = System.currentTimeMillis();
			Key key = entry.getKey();
			Value value = entry.getValue();
			if (((!checkInterval || (current - key.getLastWriteTime())
					/ StatLogSettings.getIntervalUnit() > key
						.getIntervalMultiple()))
					&& (value.getLongValue() > 0 || value.getDoubleValue() > 0)) {
				key.setLastWriteTime(current);
				output(key, value);
				temp.put(key,
						new Value(value.getLongValue(), value.getDoubleValue()));
			}
		}
		for (Map.Entry<Key, Value> entry : temp.entrySet()) {
			Key key = entry.getKey();
			Value value = data.get(key);
			value.add(-entry.getValue().getLongValue(), -entry.getValue()
					.getDoubleValue());
		}
	}

	private synchronized static void output(Key key, Value value) {
		StringBuilder sb = new StringBuilder();
		sb.append(key);
		sb.append(StatLogSettings.TOKEN);
		sb.append(value);
		sb.append(StatLogSettings.TOKEN);
		sb.append(sysdate());
		try {
			if (lastDate == null || !DateUtils.isSameDay(new Date(), lastDate)) {
				lastDate = new Date();
				if (bufferedWriter != null)
					bufferedWriter.close();
				if (fileOutputStream != null)
					fileOutputStream.close();
				fileOutputStream = new FileOutputStream(
						StatLogSettings.getLogFile(StatLogSettings.STAT_LOG_FILE_NAME
								+ DateUtils.format(lastDate,
										StatLogSettings.DATE_STYLE)), true);
				bufferedWriter = new BufferedWriter(new OutputStreamWriter(
						fileOutputStream, StatLogSettings.ENCODING));
			}
			bufferedWriter.write(sb.toString());
			bufferedWriter.write('\n');
			bufferedWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static String sysdate() {
		return String.valueOf(System.currentTimeMillis());
	}

	private static Value getValue(Key key) {
		Value value = data.get(key);
		if (value == null) {
			data.putIfAbsent(key, new Value(0));
			value = data.get(key);
		}
		return value;
	}

	public static Number[] add(Key key, long c, double d) {
		if (!writeThread.isAlive())
			runWriteThread();
		Value value = getValue(key);
		return value.add(c, d);
	}

	public static long add(Key key, long c) {
		return (Long) add(key, c, 0)[0];
	}

	public static double add(Key key, double d) {
		return (Double) add(key, 0, d)[1];
	}

	public static long add(String... names) {
		return add(new Key(names), 1);
	}

	public static long add(Key key) {
		return add(key, 1);
	}

}
