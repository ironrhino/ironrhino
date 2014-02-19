package org.ironrhino.core.stat.analysis;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.stat.Key;
import org.ironrhino.core.stat.KeyValuePair;
import org.ironrhino.core.stat.StatLogSettings;
import org.ironrhino.core.stat.Value;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.DateUtils;
import org.ironrhino.core.util.TextFileIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractAnalyzer<T> implements Analyzer<T> {

	protected Logger log = LoggerFactory.getLogger(getClass());

	protected Iterator<? extends KeyValuePair> iterator;

	public AbstractAnalyzer(boolean localhost) throws FileNotFoundException {
		this(new Date(), localhost);
	}

	public AbstractAnalyzer(Date date, boolean localhost)
			throws FileNotFoundException {
		this.iterator = newIterator(getLogFile(date, localhost).values()
				.toArray(new File[0]));
	}

	public AbstractAnalyzer(File file) throws FileNotFoundException {
		this.iterator = newIterator(new File[] { file });
	}

	public AbstractAnalyzer(Date[] dates, boolean localhost)
			throws FileNotFoundException {
		List<File> list = new ArrayList<File>();
		for (int i = 0; i < dates.length; i++)
			list.addAll(getLogFile(dates[i], localhost).values());
		this.iterator = newIterator(list.toArray(new File[0]));
	}

	public AbstractAnalyzer(Date start, Date end, boolean localhost)
			throws FileNotFoundException {
		List<File> list = new ArrayList<File>();
		Calendar cal = Calendar.getInstance();
		cal.setTime(end);
		int endDay = cal.get(Calendar.DAY_OF_YEAR);
		cal.setTime(start);
		int startDay = cal.get(Calendar.DAY_OF_YEAR);
		for (int i = 0; i < (endDay - startDay + 1); i++) {
			cal.add(Calendar.DAY_OF_YEAR, i);
			list.addAll(getLogFile(cal.getTime(), localhost).values());
		}
		this.iterator = newIterator(list.toArray(new File[0]));
	}

	public AbstractAnalyzer(Iterator<? extends KeyValuePair> iterator) {
		this.iterator = iterator;
	}

	protected Iterator<KeyValuePair> newIterator(File[] files)
			throws FileNotFoundException {
		for (File f : files)
			if (!f.exists())
				throw new FileNotFoundException(f.getAbsolutePath());
		return new TextFileIterator<KeyValuePair>(StatLogSettings.ENCODING,
				files) {

			@Override
			protected KeyValuePair transform(String line, File f) {
				String[] array = line.split("\\|");
				Key key = Key.fromString(array[0]);
				Value value = Value.fromString(array[1]);
				Date date = null;
				if (StringUtils.isNumeric(array[2])) {
					date = new Date(Long.valueOf(array[2]));
				} else {
					try {
						date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
								.parse(array[2]);
					} catch (Exception e) {

					}
				}
				return new KeyValuePair(key, value, date, getHost(f));
			}
		};
	}

	@Override
	public void analyze() {
		preAnalyze();
		try {
			Iterator<? extends KeyValuePair> it = iterate();
			while (it.hasNext())
				process(it.next());
			postAnalyze();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public Iterator<? extends KeyValuePair> iterate() {
		return this.iterator;
	}

	protected void preAnalyze() {

	}

	protected void postAnalyze() {

	}

	protected abstract void process(KeyValuePair pair);

	// host,file pair
	public static Map<String, File> getLogFile(Date date,
			final boolean localhost) {
		final Map<String, File> map = new TreeMap<String, File>();
		boolean today = DateUtils.isToday(date);
		StringBuilder sb = new StringBuilder();
		if (localhost)
			sb.append(AppInfo.getHostName());
		sb.append(StatLogSettings.SEPARATOR);
		sb.append(StatLogSettings.STAT_LOG_FILE_NAME);
		if (!today)
			sb.append(new SimpleDateFormat(StatLogSettings.DATE_STYLE)
					.format(date));
		final String suffix = sb.toString();
		File dir = StatLogSettings.getLogFileDirectory();
		dir.listFiles(new FileFilter() {
			@Override
			public boolean accept(File f) {
				String name = f.getName();
				if (localhost && name.equals(suffix)) {
					map.put(name.substring(0,
							name.lastIndexOf(StatLogSettings.SEPARATOR)), f);
					return true;
				} else if (name.endsWith(suffix)) {
					map.put(name.substring(0, name.lastIndexOf(suffix)), f);
					return true;
				}
				return false;
			}
		});
		return map;
	}

	public static boolean hasLogFile(Date date, boolean localhost) {
		return getLogFile(date, localhost).size() > 0;
	}

	public static String getHost(File file) {
		String name = file.getName();
		return name.substring(0, name.lastIndexOf(StatLogSettings.SEPARATOR));
	}
}