package org.ironrhino.core.servlet;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.Query;
import javax.servlet.ServletContext;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.ReflectionUtils;
import org.springframework.beans.BeanWrapperImpl;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ServletContainerHelper {

	public static String getServerInfo(ServletContext servletContext) {
		String serverInfo = servletContext.getServerInfo();
		if (StringUtils.isBlank(serverInfo))
			return null;
		if (serverInfo.startsWith("Apache Tomcat/")) {
			// Apache Tomcat/9.0.11
			serverInfo = serverInfo.substring(serverInfo.indexOf(' ') + 1);
		} else if (serverInfo.startsWith("jetty/")) {
			// jetty/9.4.z-SNAPSHOT
			// jetty/9.4.8.v20171121
			int index = serverInfo.indexOf('-');
			if (index > 0)
				serverInfo = serverInfo.substring(0, index);
			else
				serverInfo = serverInfo.substring(0, serverInfo.lastIndexOf('.'));
			serverInfo = StringUtils.capitalize(serverInfo);
		} else if (serverInfo.startsWith("WildFly ")) {
			// WildFly Servlet 14.0.1.Final (WildFly Core 6.0.2.Final) - 2.0.13.Final
			// WildFly Full 14.0.1.Final (WildFly Core 6.0.2.Final) - 2.0.13.Final
			String[] arr = serverInfo.split("\\s");
			String version = arr[2];
			if (version.endsWith(".Final"))
				version = version.substring(0, version.lastIndexOf('.'));
			serverInfo = arr[0] + '/' + version;
		} else if (serverInfo.startsWith("GlassFish ")) {
			// GlassFish Server Open Source Edition 5.0.1
			String[] arr = serverInfo.split("\\s");
			serverInfo = arr[0] + '/' + arr[arr.length - 1];
		} else if (serverInfo.startsWith("Resin/")) {
			// Resin/4.0.55
		} else if (serverInfo.startsWith("WebLogic ")) {
			// WebLogic Server 12.2.1.2.0 Mon Oct 3 04:35:36 PDT 2016 1827450
			String[] arr = serverInfo.split("\\s");
			serverInfo = arr[0] + '/' + arr[2];
		} else if (serverInfo.startsWith("IBM WebSphere")) {
			// IBM WebSphere Liberty/18.0.0.2
			serverInfo = "WebSphere" + serverInfo.substring(serverInfo.lastIndexOf('/'));
		}
		return serverInfo;
	}

	@SuppressWarnings("unchecked")
	public static int detectHttpPort(ServletContext servletContext, boolean ssl) {
		String className = servletContext.getClass().getName();
		if (className.startsWith("org.apache.catalina.")) {
			// tomcat or glassfish
			try {
				// detect via jmx
				MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
				Set<ObjectName> objs = mbs.queryNames(new ObjectName("*:type=Connector,port=*"),
						Query.match(Query.attr("scheme"), Query.value(ssl ? "https" : "http")));
				for (ObjectName on : objs) {
					if (((String) mbs.getAttribute(on, "protocol")).toLowerCase(Locale.ROOT).startsWith("http/"))
						return (Integer) mbs.getAttribute(on, "port");
				}
			} catch (Exception e) {
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
							if (connector.getClass().getMethod("getScheme").invoke(connector)
									.equals(ssl ? "https" : "http") && (name == null || !name.contains("admin")))
								return (Integer) connector.getClass().getMethod("getPort").invoke(connector);
						}
					}
				}
			} catch (Throwable e) {
			}
		} else if (className.startsWith("org.eclipse.jetty.")) {
			// jetty
			try {
				if (ssl) {
					String port = System.getProperty("jetty.ssl.port");
					if (StringUtils.isNotBlank(port))
						return Integer.valueOf(port);
					else
						return 0;
				}
				Object webAppContext = ReflectionUtils.getFieldValue(servletContext, "this$0");
				Object server = ReflectionUtils.getFieldValue(webAppContext, "_server");
				Object[] connectors = (Object[]) server.getClass().getMethod("getConnectors").invoke(server);
				for (Object connector : connectors) {
					List<String> protocols = (List<String>) connector.getClass().getMethod("getProtocols")
							.invoke(connector);
					boolean http = false;
					for (String p : protocols)
						if (p.toLowerCase(Locale.ROOT).startsWith("http/")) {
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
		} else if (className.startsWith("io.undertow.servlet.")) {
			// wildfly
			try {
				MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
				return (Integer) mbs.getAttribute(new ObjectName(
						"jboss.as:socket-binding-group=standard-sockets,socket-binding=" + (ssl ? "https" : "http")),
						"boundPort");
			} catch (Exception e) {
			}
		} else if (className.startsWith("com.caucho.server.")) {
			// resin
			try {
				Class<?> configClass = servletContext.getClassLoader().loadClass("com.caucho.config.Config");
				Object server = configClass.getMethod("getProperty", String.class).invoke(null, "server");
				return (Integer) server.getClass().getMethod(ssl ? "getHttpsPort" : "getHttpPort").invoke(server);
			} catch (Throwable e) {
			}
		} else if (className.startsWith("weblogic.servlet.")) {
			// weblogic
			try {
				MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
				Set<ObjectName> objs = mbs.queryNames(new ObjectName("com.bea:Type=ServerRuntime,*"), null);
				for (ObjectName on : objs) {
					try {
						return (Integer) mbs.getAttribute(on, ssl ? "SSLListenPort" : "ListenPort");
					} catch (AttributeNotFoundException e) {
					}
				}
			} catch (Exception e) {
			}
		} else if (className.startsWith("com.ibm.ws.")) {
			// websphere
			try {
				MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
				Set<ObjectName> objs = mbs.queryNames(
						new ObjectName("WebSphere:type=endpoint,name=defaultHttpEndpoint" + (ssl ? "-ssl" : "") + ",*"),
						null);
				for (ObjectName on : objs)
					try {
						return (Integer) mbs.getAttribute(on, "Port");
					} catch (AttributeNotFoundException e) {
					}
			} catch (Exception e) {
			}
		}
		return 0;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void addErrorPages(ServletContext servletContext, Map<Integer, String> errorPages) {
		try {
			String className = servletContext.getClass().getName();
			if (className.startsWith("org.apache.catalina.")) {
				Object ctx = ReflectionUtils.getFieldValue(servletContext, "context");
				Object standardContext;
				Method m;
				try {
					m = ctx.getClass().getDeclaredMethod("getContext");
					m.setAccessible(true);
					standardContext = m.invoke(ctx);
				} catch (NoSuchMethodException e) {
					// tomcat7 or glassfish
					standardContext = ReflectionUtils.getFieldValue(ctx, "context");
				}
				Class<?> errorPageClass = standardContext.getClass().getMethod("findErrorPage", int.class)
						.getReturnType();
				m = standardContext.getClass().getMethod("addErrorPage", errorPageClass);
				for (Map.Entry<Integer, String> entry : errorPages.entrySet()) {
					Object errorPage = errorPageClass.getConstructor().newInstance();
					BeanWrapperImpl bw = new BeanWrapperImpl(errorPage);
					bw.setPropertyValue("errorCode", entry.getKey());
					bw.setPropertyValue("location", entry.getValue());
					m.invoke(standardContext, errorPage);
				}
			} else if (className.startsWith("org.eclipse.jetty.")) {
				Object ctx = ReflectionUtils.getFieldValue(servletContext, "this$0");
				Object errorHandler = ctx.getClass().getMethod("getErrorHandler").invoke(ctx);
				Method m = errorHandler.getClass().getMethod("addErrorPage", int.class, String.class);
				for (Map.Entry<Integer, String> entry : errorPages.entrySet())
					m.invoke(errorHandler, entry.getKey(), entry.getValue());
			} else if (className.startsWith("io.undertow.servlet.")) {
				Object deployment = servletContext.getClass().getMethod("getDeployment").invoke(servletContext);
				Object deploymentInfo = deployment.getClass().getMethod("getDeploymentInfo").invoke(deployment);
				for (Method m : deploymentInfo.getClass().getMethods()) {
					if (m.getName().equals("addErrorPage")) {
						Class<?> errorPageClass = m.getParameterTypes()[0];
						for (Map.Entry<Integer, String> entry : errorPages.entrySet())
							m.invoke(deploymentInfo, errorPageClass.getConstructor(String.class, int.class)
									.newInstance(entry.getValue(), entry.getKey()));
						break;
					}
				}
			} else if (className.startsWith("weblogic.servlet.")) {
				Object errorManager = servletContext.getClass().getMethod("getErrorManager").invoke(servletContext);
				Method m = errorManager.getClass().getDeclaredMethod("registerError", int.class, String.class);
				m.setAccessible(true);
				for (Map.Entry<Integer, String> entry : errorPages.entrySet())
					m.invoke(errorManager, entry.getKey(), entry.getValue());
			} else if (className.startsWith("com.ibm.ws.")) {
				Object webAppConfig = ReflectionUtils.getFieldValue(servletContext, "webAppConfig");
				Map codeErrorPages = (Map) webAppConfig.getClass().getMethod("getCodeErrorPages").invoke(webAppConfig);
				Class<?> errorPageClass = webAppConfig.getClass().getMethod("getErrorPageByErrorCode", Integer.class)
						.getReturnType();
				for (Map.Entry<Integer, String> entry : errorPages.entrySet())
					codeErrorPages.put(entry.getKey(),
							errorPageClass.getConstructor(String.class).newInstance(entry.getValue()));
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

}