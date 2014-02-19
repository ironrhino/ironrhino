package org.ironrhino.common.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.URL;

import org.ironrhino.core.util.AppInfo;

public class LocationParser {

	static class Holder {
		static LocationParser instance = new LocationParser();
	}

	private static final int ADDRESS_RECORD_LENGTH = 7;

	private static final byte AREA_FOLLOWED = 0x01;

	private static final byte NO_AREA = 0x2;

	private RandomAccessFile file;

	private long start, end;

	private byte[] buf;

	private byte[] b4;

	private byte[] b3;

	private boolean available;

	private LocationParser() {
		buf = new byte[100];
		b4 = new byte[4];
		b3 = new byte[3];
		try {
			File f = new File(AppInfo.getAppHome() + "/data/wry.dat");
			if (f.exists()) {
				file = new RandomAccessFile(f, "r");
			} else {
				ClassLoader cl = Thread.currentThread().getContextClassLoader();
				if (cl == null)
					cl = LocationParser.class.getClassLoader();
				URL url = cl.getResource("resources/data/wry.dat");
				if (url != null)
					file = new RandomAccessFile(url.getFile(), "r");
			}
			start = readLong4(0);
			end = readLong4(4);
			if (start == -1 || end == -1) {
				file.close();
				file = null;
			}
			available = true;
		} catch (Exception e) {
			file = null;
			e.printStackTrace();
		}
	}

	public static Location parseLocal(String host) {
		try {
			LocationParser instance = Holder.instance;
			if (instance == null || !instance.available)
				return null;
			String[] loc = instance.parseLocation(InetAddress.getByName(host)
					.getAddress());
			String string = (loc[0] != null ? loc[0] : "")
					+ (loc[1] != null ? loc[1] : "");
			Location location = new Location(string);
			for (String s : LocationUtils.autonomousRegions)
				if (string.startsWith(s)) {
					location.setFirstArea(s);
					if (string.indexOf("自治区") > 0)
						location.setSecondArea(string.substring(
								string.indexOf("自治区") + 3, string.indexOf("市")));
					else
						location.setSecondArea(string.substring(s.length(),
								string.indexOf("市")));
					if (string.indexOf("区") > string.indexOf("市"))
						location.setThirdArea(string.substring(
								string.indexOf("市") + 1, string.indexOf("区")));
					return location;
				}

			for (String s : LocationUtils.specialAdministrativeRegions)
				if (string.startsWith(s)) {
					location.setFirstArea(s);
					return location;
				}

			if (string.indexOf("省") > 0) {
				location.setFirstArea(string.substring(0, string.indexOf("省")));
				if (string.indexOf("市") > string.indexOf("省") + 1)
					location.setSecondArea(string.substring(
							string.indexOf("省") + 1, string.indexOf("市")));
				if (string.indexOf("区") > string.indexOf("市") + 1)
					location.setThirdArea(string.substring(
							string.indexOf("市") + 1, string.indexOf("区")));
				return location;
			} else {
				if (string.indexOf("市") > 0)
					location.setFirstArea(string.substring(0,
							string.indexOf("市")));
				if (string.indexOf("区") > string.indexOf("市") + 1)
					location.setSecondArea(string.substring(
							string.indexOf("市") + 1, string.indexOf("区")));
				if (string.indexOf("县") > string.indexOf("市") + 1)
					location.setSecondArea(string.substring(
							string.indexOf("县") + 1, string.indexOf("区")));
				return location;
			}
		} catch (Exception e) {
			return null;
		}
	}

	private String[] parseLocation(byte[] address) throws Exception {
		String[] loc = new String[2];
		long offset = locateAddress(address);
		file.seek(offset + 4);
		byte b = file.readByte();
		if (b == AREA_FOLLOWED) {
			long countryOffset = readLong3();
			file.seek(countryOffset);
			b = file.readByte();
			if (b == NO_AREA) {
				loc[0] = readString(readLong3());
				file.seek(countryOffset + 4);
			} else
				loc[0] = readString(countryOffset);
			loc[1] = readArea(file.getFilePointer());
		} else if (b == NO_AREA) {
			loc[0] = readString(readLong3());
			loc[1] = readArea(offset + 8);
		} else {
			loc[0] = readString(file.getFilePointer() - 1);
			loc[1] = readArea(file.getFilePointer());
		}
		return loc;
	}

	private long readLong4(long offset) {
		long ret = 0;
		try {
			file.seek(offset);
			ret |= (file.readByte() & 0xFF);
			ret |= ((file.readByte() << 8) & 0xFF00);
			ret |= ((file.readByte() << 16) & 0xFF0000);
			ret |= ((file.readByte() << 24) & 0xFF000000);
			return ret;
		} catch (Exception e) {
			return -1;
		}
	}

	private long readLong3(long offset) {
		long ret = 0;
		try {
			file.seek(offset);
			file.readFully(b3);
			ret |= (b3[0] & 0xFF);
			ret |= ((b3[1] << 8) & 0xFF00);
			ret |= ((b3[2] << 16) & 0xFF0000);
			return ret;
		} catch (IOException e) {
			return -1;
		}
	}

	private long readLong3() {
		long ret = 0;
		try {
			file.readFully(b3);
			ret |= (b3[0] & 0xFF);
			ret |= ((b3[1] << 8) & 0xFF00);
			ret |= ((b3[2] << 16) & 0xFF0000);
			return ret;
		} catch (IOException e) {
			return -1;
		}
	}

	private void readAddress(long offset, byte[] address) {
		try {
			file.seek(offset);
			file.readFully(address);
			byte temp = address[0];
			address[0] = address[3];
			address[3] = temp;
			temp = address[1];
			address[1] = address[2];
			address[2] = temp;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private int compareAddress(byte[] address, byte[] beginAddress) {
		for (int i = 0; i < 4; i++) {
			int r = compareByte(address[i], beginAddress[i]);
			if (r != 0)
				return r;
		}
		return 0;
	}

	private int compareByte(byte b1, byte b2) {
		if ((b1 & 0xFF) > (b2 & 0xFF))
			return 1;
		else if ((b1 ^ b2) == 0)
			return 0;
		else
			return -1;
	}

	private long locateAddress(byte[] address) {
		long m = 0;
		int r;
		readAddress(start, b4);
		r = compareAddress(address, b4);
		if (r == 0)
			return start;
		else if (r < 0)
			return -1;
		for (long i = start, j = end; i < j;) {
			m = getMiddleOffset(i, j);
			readAddress(m, b4);
			r = compareAddress(address, b4);
			if (r > 0)
				i = m;
			else if (r < 0) {
				if (m == j) {
					j -= ADDRESS_RECORD_LENGTH;
					m = j;
				} else
					j = m;
			} else
				return readLong3(m + 4);
		}
		m = readLong3(m + 4);
		readAddress(m, b4);
		r = compareAddress(address, b4);
		if (r <= 0)
			return m;
		else
			return -1;
	}

	private long getMiddleOffset(long begin, long end) {
		long records = (end - begin) / ADDRESS_RECORD_LENGTH;
		records >>= 1;
		if (records == 0)
			records = 1;
		return begin + records * ADDRESS_RECORD_LENGTH;
	}

	private String readArea(long offset) throws Exception {
		file.seek(offset);
		byte b = file.readByte();
		if (b == 0x01 || b == 0x02) {
			long areaOffset = readLong3(offset + 1);
			if (areaOffset == 0)
				return null;
			else
				return readString(areaOffset);
		} else
			return readString(offset);
	}

	private String readString(long offset) throws Exception {
		file.seek(offset);
		int i;
		for (i = 0, buf[i] = file.readByte(); buf[i] != 0; buf[++i] = file
				.readByte())
			;
		if (i != 0)
			return new String(buf, 0, i, "GBK");
		return null;
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		if (file != null)
			file.close();
	}

}
