package org.ironrhino.core.stat;

import java.io.File;

import org.ironrhino.core.util.AppInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatLogSettings {

	public static final String WRITETHREAD_NAME = "STATLOG-WRITE";

	public static final String ENCODING = "UTF-8";

	public static final String DATE_STYLE = "'.'yyyy-MM-dd";

	public static final String LAYOUT = "%m%n";

	public static final String TOKEN = "|";

	public static final String SEPARATOR = "_";

	public static final String FILE_DIRECTORY = "/stat";

	public static final String STAT_LOG_FILE_NAME = "stat.log";

	private static final Logger log = LoggerFactory
			.getLogger(StatLogSettings.class);

	private static int intervalUnit = 60; // senconds

	public static int getIntervalUnit() {
		return intervalUnit;
	}

	public static void setIntervalUnit(int intervalUnit) {
		if (intervalUnit > 0)
			StatLogSettings.intervalUnit = intervalUnit;
	}

	public static File getLogFileDirectory() {
		File dir = new File(AppInfo.getAppHome() + FILE_DIRECTORY);
		if (!dir.exists() && dir.mkdirs())
			log.error("mkdir error:" + dir.getAbsolutePath());
		return dir;
	}

	public static String getLogFile(String filename) {
		return new File(getLogFileDirectory(), AppInfo.getHostName()
				+ SEPARATOR + filename).getAbsolutePath();
	}

}
