package org.ironrhino.core.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class ExceptionUtils {
	public static String getStackTraceAsString(Throwable t) {
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			PrintStream ps = new PrintStream(os, true, "UTF-8");
			t.printStackTrace(ps);
			ps.flush();
			ps.close();
			String s = os.toString("UTF-8");
			os.flush();
			os.close();
			return s;
		} catch (IOException e) {
			return t.getCause().toString();
		}
	}

	public static String getRootMessage(Throwable t) {
		int maxDepth = 10;
		while (t.getCause() != null && maxDepth > 0) {
			maxDepth--;
			t = t.getCause();
		}
		return t.getMessage();
	}

	public static String getDetailMessage(Throwable t) {
		StringBuilder sb = new StringBuilder();
		sb.append(t.getClass().getName()).append(":").append(t.getMessage());
		int maxDepth = 10;
		while (t.getCause() != null && maxDepth > 0) {
			maxDepth--;
			t = t.getCause();
			sb.append("\n").append(t.getClass().getName());
			if (sb.indexOf(t.getMessage()) < 0)
				sb.append(":").append(t.getMessage());
		}
		return sb.toString();
	}
}
