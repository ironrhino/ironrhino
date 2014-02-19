package org.ironrhino.core.stat;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.slf4j.LoggerFactory;

public class StatLog {

	private static final Logger statLogger = Logger.getLogger("Stat");

	private static final org.slf4j.Logger log = LoggerFactory
			.getLogger(StatLog.class);

	private static final Lock timerLock = new ReentrantLock();

	private static final Condition condition = timerLock.newCondition();

	private static final ConcurrentHashMap<Key, Value> data = new ConcurrentHashMap<Key, Value>(
			64);

	private static Thread writeThread;

	private static FileAppender statLogAppender;

	static {
		PatternLayout layout = new PatternLayout(StatLogSettings.LAYOUT);
		try {
			DailyRollingFileAppender statAppender = new DailyRollingFileAppender(
					layout,
					StatLogSettings
							.getLogFile(StatLogSettings.STAT_LOG_FILE_NAME),
					StatLogSettings.DATE_STYLE);
			statAppender.setAppend(true);
			statAppender.setEncoding(StatLogSettings.ENCODING);
			statAppender.activateOptions();
			statLogger.addAppender(statAppender);
			statLogger.setLevel(Level.INFO);
			statLogger.setAdditivity(false);

		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
		startNewThread();
		Runtime.getRuntime().addShutdownHook(new Thread() {

			@Override
			public void run() {
				write(false);
				if (statLogAppender != null)
					statLogAppender.close();
			}
		});
	}

	private static void runWriteThread() {
		if (writeThread.isAlive())
			return;
		try {
			writeThread.interrupt();
		} catch (Exception e) {
			log.error("interrupt write thread error", e);
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
						if (condition.await(StatLogSettings.getIntervalUnit(),
								TimeUnit.SECONDS))
							log.debug("await returns true");
					} catch (Exception e) {
						log.error("wait error", e);
					} finally {
						timerLock.unlock();
					}
					StatLog.write();
				}
			}

		}, StatLogSettings.WRITETHREAD_NAME);
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
				output(statLogger, key, value);
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

	private static void output(Logger logger, Key key, Value value) {
		StringBuilder sb = new StringBuilder();
		sb.append(key);
		sb.append(StatLogSettings.TOKEN);
		sb.append(value);
		sb.append(StatLogSettings.TOKEN);
		sb.append(sysdate());
		logger.info(sb.toString());
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
