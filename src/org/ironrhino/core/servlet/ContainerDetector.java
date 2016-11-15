package org.ironrhino.core.servlet;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.Query;
import javax.servlet.ServletContext;

import org.ironrhino.core.util.ReflectionUtils;

public class ContainerDetector {

	@SuppressWarnings("unchecked")
	public static int port(ServletContext servletContext) {
		String className = servletContext.getClass().getName();
		if (className.startsWith("org.apache.catalina")) {
			// tomcat or glassfish
			try {
				// detect via jmx
				MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
				Set<ObjectName> objs = mbs.queryNames(new ObjectName("*:type=Connector,*"),
						Query.match(Query.attr("scheme"), Query.value("http")));
				for (ObjectName on : objs) {
					if (((String) mbs.getAttribute(on, "protocol")).toLowerCase().startsWith("http/"))
						return (Integer) mbs.getAttribute(on, "port");
				}
			} catch (Throwable e) {
			}
			try {
				// detect via reflection
				Object o = ReflectionUtils.getFieldValue(servletContext, "context");
				Object container = ReflectionUtils.getFieldValue(o, "context");
				Method getParent = container.getClass().getMethod("getParent");
				Object c = getParent.invoke(container);
				Class<?> standardEngineClass = Class.forName("org.apache.catalina.core.StandardEngine");
				while (c != null && !(standardEngineClass.isAssignableFrom(c.getClass())))
					c = getParent.invoke(c);
				if (c != null) {
					Object service = standardEngineClass.getMethod("getService").invoke(c);
					Object[] connectors = (Object[]) service.getClass().getMethod("findConnectors").invoke(service);
					if (connectors != null && connectors.length > 0) {
						for (Object connector : connectors) {
							String name = null;
							try {
								name = (String) connector.getClass().getMethod("getName").invoke(connector);
							} catch (NoSuchMethodException e) {
								try {
									name = (String) connector.getClass().getMethod("getExecutorName").invoke(connector);
								} catch (NoSuchMethodException e2) {

								}
							}
							if (connector.getClass().getMethod("getScheme").invoke(connector).equals("http")
									&& (name == null || !name.contains("admin")))
								return (Integer) connector.getClass().getMethod("getPort").invoke(connector);
						}
					}
				}
			} catch (Throwable e) {
			}
		}
		if (className.startsWith("org.eclipse.jetty")) {
			// jetty
			try {
				Object webAppContext = ReflectionUtils.getFieldValue(servletContext, "this$0");
				Object server = ReflectionUtils.getFieldValue(webAppContext, "_server");
				Object[] connectors = (Object[]) server.getClass().getMethod("getConnectors").invoke(server);
				for (Object connector : connectors) {
					List<String> protocols = (List<String>) connector.getClass().getMethod("getProtocols")
							.invoke(connector);
					boolean http = false;
					for (String p : protocols)
						if (p.toLowerCase().startsWith("http/")) {
							http = true;
							break;
						}
					if (http) {
						int port = (Integer) connector.getClass().getMethod("getLocalPort").invoke(connector);
						if (port <= 0)
							port = (Integer) connector.getClass().getMethod("getPort").invoke(connector);
						return port;
					}
				}
			} catch (Throwable e) {
			}
		}
		if (className.startsWith("io.undertow.servlet")) {
			// wildfly
			try {
				MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
				return (Integer) mbs.getAttribute(
						new ObjectName("jboss.as:socket-binding-group=standard-sockets,socket-binding=http"),
						"boundPort");
			} catch (Throwable e) {
			}
		}
		if (className.startsWith("com.caucho.server")) {
			// resin
			try {
				Class<?> configClass = servletContext.getClassLoader().loadClass("com.caucho.config.Config");
				Object server = configClass.getMethod("getProperty", String.class).invoke(null, "server");
				return (Integer) server.getClass().getMethod("getHttpPort").invoke(server);
			} catch (Throwable e) {
			}
		}
		return 0;
	}

}