package org.ironrhino.common.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.ironrhino.core.util.AppInfo;

public class LocationParser {

	/*
	 * https://github.com/lionsoul2014/ip2region/
	 */
	private static final String FILE_LOCATION = "/data/ip2region.db";
	private static final int INDEX_BLOCK_LENGTH = 12;

	private final byte[] data;
	private final long firstIndex;
	private final long lastIndex;
	private final int totalIndexBlocks;

	private LocationParser() {
		byte[] bytes;
		try {
			InputStream is;
			File f = new File(AppInfo.getAppHome() + FILE_LOCATION);
			if (f.exists()) {
				is = new FileInputStream(f);
			} else {
				is = LocationParser.class.getClassLoader().getResourceAsStream("resources" + FILE_LOCATION);
			}
			if (is != null) {
				bytes = new byte[is.available()];
				IOUtils.readFully(is, bytes);
				is.close();
			} else {
				bytes = null;
			}
		} catch (Exception e) {
			bytes = null;
			e.printStackTrace();
		}
		data = bytes;
		firstIndex = getIntLong(data, 0);
		lastIndex = getIntLong(data, 4);
		totalIndexBlocks = (int) ((lastIndex - firstIndex) / INDEX_BLOCK_LENGTH) + 1;
	}

	private String doParse(String input) {
		if (data == null)
			return null;
		String[] arr = input.split("\\.");
		if (arr.length != 4)
			return null;
		long ip = ((((Integer.valueOf(arr[0]) << 24) & 0xFF000000) | ((Integer.valueOf(arr[1]) << 16) & 0x00FF0000)
				| ((Integer.valueOf(arr[2]) << 8) & 0x0000FF00) | ((Integer.valueOf(arr[3]) << 0) & 0x000000FF))
				& 0xFFFFFFFFL);

		int l = 0, h = totalIndexBlocks;
		long start, end, index = 0;
		while (l <= h) {
			int m = (l + h) >> 1;
			int p = (int) (firstIndex + m * INDEX_BLOCK_LENGTH);
			start = getIntLong(data, p);
			if (ip < start) {
				h = m - 1;
			} else {
				end = getIntLong(data, p + 4);
				if (ip > end) {
					l = m + 1;
				} else {
					index = getIntLong(data, p + 8);
					break;
				}
			}
		}
		if (index == 0)
			return null;
		return new String(data, (int) ((index & 0x00FFFFFF)) + 4, (int) ((index >> 24) & 0xFF) - 4,
				StandardCharsets.UTF_8);
	}

	public static Location parse(String ip) {
		String region = getSharedInstance().doParse(ip.trim());
		if (region == null)
			return null;
		String[] loc = region.split("\\|");
		if (loc[0].equals("香港") || loc[0].equals("澳门") || loc[0].equals("台湾")) {
			Location location = new Location(loc[0]);
			location.setFirstArea(loc[0]);
			location.setSecondArea(loc[0]);
			return location;
		} else if (!loc[0].equals("中国") || loc.length < 4)
			return null;
		String firstArea = loc[2];
		String secondArea = loc[3];
		Location location = new Location(secondArea.equals(firstArea) ? secondArea : firstArea + secondArea);
		location.setFirstArea(LocationUtils.shortenName(firstArea));
		location.setSecondArea(LocationUtils.shortenName(secondArea));
		return location;
	}

	private static volatile LocationParser sharedInstance;

	private static LocationParser getSharedInstance() {
		LocationParser temp = sharedInstance;
		if (temp == null) {
			synchronized (LocationParser.class) {
				temp = sharedInstance;
				if (temp == null) {
					temp = new LocationParser();
					sharedInstance = temp;
				}
			}
		}
		return temp;
	}

	private static long getIntLong(byte[] bytes, int offset) {
		if (bytes == null)
			return 0;
		return (((bytes[offset++] & 0x000000FFL)) | ((bytes[offset++] << 8) & 0x0000FF00L)
				| ((bytes[offset++] << 16) & 0x00FF0000L) | ((bytes[offset] << 24) & 0xFF000000L));
	}

}
