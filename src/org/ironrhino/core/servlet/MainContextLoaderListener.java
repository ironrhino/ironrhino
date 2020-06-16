package org.ironrhino.core.servlet;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.annotation.WebListener;

import org.apache.logging.log4j.LogManager;
import org.ironrhino.core.util.HttpClientUtils;
import org.ironrhino.core.util.ReflectionUtils;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.ContextLoaderListener;

/**
 * 
 * Section 4.4 of the Servlet 3.0 specification does not permit configuration
 * methods to be called from a ServletContextListener that was not defined in
 * web.xml, a web-fragment.xml file nor annotated with @WebListener
 *
 */
@WebListener("Must declared as @WebListener according to Section 4.4")
public class MainContextLoaderListener extends ContextLoaderListener {

	public static final String CONFIG_LOCATION = "classpath*:resources/spring/applicationContext-*.xml";

	@Override
	public void contextInitialized(ServletContextEvent event) {
		ServletContext sc = event.getServletContext();
		sc.setInitParameter(CONFIG_LOCATION_PARAM, CONFIG_LOCATION);
		super.contextInitialized(event);
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		super.contextDestroyed(event);
		cleanup();
	}

	protected void cleanup() {
		try {
			HttpClientUtils.getDefaultInstance().close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		LogManager.shutdown();
		cleanupJdbcDrivers();
		cancelTimers();
		cleanupThreadLocals();
	}

	protected void cleanupJdbcDrivers() {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		try {
			String className = "com.mysql.cj.jdbc.AbandonedConnectionCleanupThread";
			String methodName = "checkedShutdown";
			if (ClassUtils.isPresent(className, cl)) {
				ClassUtils.forName(className, cl).getMethod(methodName).invoke(null);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		try {
			String className = "com.mysql.jdbc.AbandonedConnectionCleanupThread";
			String methodName = "checkedShutdown";
			if (ClassUtils.isPresent(className, cl)) {
				ClassUtils.forName(className, cl).getMethod(methodName).invoke(null);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		Enumeration<Driver> drivers = DriverManager.getDrivers();
		while (drivers.hasMoreElements()) {
			Driver driver = drivers.nextElement();
			try {
				DriverManager.deregisterDriver(driver);
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
		}
	}

	protected void cancelTimers() {
		try {
			for (Thread thread : Thread.getAllStackTraces().keySet())
				if (thread.getClass().getSimpleName().equals("TimerThread"))
					cancelTimer(thread);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	private void cancelTimer(Thread thread) throws Exception {
		// Timer::cancel
		Object queue = ReflectionUtils.getFieldValue(thread, "queue");
		synchronized (queue) {
			ReflectionUtils.setFieldValue(thread, "newTasksMayBeScheduled", false);
			Method m = queue.getClass().getDeclaredMethod("clear");
			m.setAccessible(true);
			m.invoke(queue);
			queue.notify();
		}
	}

	protected void cleanupThreadLocals() {
		try {
			for (Thread thread : Thread.getAllStackTraces().keySet())
				cleanupThreadLocals(thread);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	private void cleanupThreadLocals(Thread thread) throws Exception {
		for (String name : "threadLocals,inheritableThreadLocals".split(",")) {
			Field f = Thread.class.getDeclaredField(name);
			f.setAccessible(true);
			f.set(thread, null);
		}
	}

}
