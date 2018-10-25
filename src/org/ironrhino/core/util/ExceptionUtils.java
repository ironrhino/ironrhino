package org.ironrhino.core.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import org.hibernate.validator.internal.engine.ConstraintViolationImpl;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ExceptionUtils {

	private static final int MAX_DEPTH = 10;

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

	public static Throwable getRootCause(Throwable t) {
		int depth = MAX_DEPTH;
		while (t.getCause() != null && depth > 0) {
			depth--;
			t = t.getCause();
		}
		return t;
	}

	public static String getRootMessage(Throwable t) {
		return getRootCause(t).getMessage();
	}

	public static String getDetailMessage(Throwable t) {
		StringBuilder sb = new StringBuilder();
		sb.append(t.getClass().getName()).append(":").append(t.getLocalizedMessage());
		int maxDepth = 10;
		while (t.getCause() != null && maxDepth > 0) {
			maxDepth--;
			t = t.getCause();
			sb.append("\n").append(t.getClass().getName());
			if (t.getLocalizedMessage() != null && sb.indexOf(t.getLocalizedMessage()) < 0)
				sb.append(":").append(t.getLocalizedMessage());
		}
		return sb.toString();
	}

	public static Throwable transformForSerialization(Throwable throwable) {
		if (throwable instanceof ConstraintViolationException) {
			ConstraintViolationException cve = (ConstraintViolationException) throwable;
			Set<ConstraintViolation<?>> cvs = new LinkedHashSet<ConstraintViolation<?>>();
			for (ConstraintViolation<?> cv : cve.getConstraintViolations()) {
				cvs.add(ConstraintViolationImpl.forBeanValidation(cv.getMessageTemplate(), null, null, cv.getMessage(),
						null, null, null, null, cv.getPropertyPath(), null, null, null));
			}
			ConstraintViolationException ex = new ConstraintViolationException(cvs);
			ex.setStackTrace(cve.getStackTrace());
			return ex;
		}
		return throwable;
	}

	public static void trimStackTrace(Throwable throwable, int maxStackTraceElements) {
		StackTraceElement[] elements = throwable.getStackTrace();
		if (elements.length > maxStackTraceElements)
			throwable.setStackTrace(Arrays.copyOfRange(elements, 0, maxStackTraceElements));
	}

	public static void fillInClientStackTraceIfPossible(Throwable ex, int maxClientStackTraceElements) {
		// org.springframework.remoting.support.RemoteInvocationUtils.fillInClientStackTraceIfPossible()
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

	@SuppressWarnings("unchecked")
	public static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
		throw (E) e;
	}

}
