package org.ironrhino.core.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;

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
			if (t.getMessage() != null && sb.indexOf(t.getMessage()) < 0)
				sb.append(":").append(t.getMessage());
		}
		return sb.toString();
	}

	public static void trimStackTrace(Throwable throwable, int maxStackTraceElements) {
		StackTraceElement[] elements = throwable.getStackTrace();
		if (elements.length > maxStackTraceElements)
			throwable.setStackTrace(Arrays.copyOfRange(elements, 0, maxStackTraceElements));
	}

	public static void fillInClientStackTraceIfPossible(Throwable ex, int maxClientStackTraceElements) {
		// org.springframework.remoting.support.RemoteInvocationUtils.fillInClientStackTraceIfPossible()
		if (ex != null) {
			if (ex != null) {
				StackTraceElement[] clientStack = new Throwable().getStackTrace();
				StackTraceElement[] serverStack = ex.getStackTrace();
				int clientStacks = Math.min(maxClientStackTraceElements, clientStack.length);
				StackTraceElement[] combinedStack = new StackTraceElement[serverStack.length + 1 + clientStacks + 1];
				System.arraycopy(serverStack, 0, combinedStack, 0, serverStack.length);
				combinedStack[serverStack.length] = new StackTraceElement("^", "^", null, 0);
				System.arraycopy(clientStack, 0, combinedStack, serverStack.length + 1, clientStacks);
				combinedStack[combinedStack.length - 1] = new StackTraceElement(".", ".", null, 0);
				ex.setStackTrace(combinedStack);
			}
		}
	}
}
