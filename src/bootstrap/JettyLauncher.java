package bootstrap;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URL;

import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHandler.Default404Servlet;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.JettyWebXmlConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebInfConfiguration;

public class JettyLauncher {

	public static void start(URL warUrl) throws Exception {
		int port = -1;
		String p = System.getProperty("port.http");
		if (p == null)
			p = System.getProperty("jetty.http.port");
		if (p != null && p.trim().length() > 0)
			port = Integer.parseInt(p);
		if (port == -1) {
			port = 8080;
			if (System.getenv("CONTAINER") == null) {
				while (getTempDirectory(port).exists() || !isAvailable(port))
					port++;
				System.setProperty("port.http", String.valueOf(port));
			}
		}
		File tempDir = getTempDirectory(port);
		tempDir.mkdirs();
		Server server = new Server(port);
		server.addBean(new QueuedThreadPool(Integer.getInteger("jetty.maxThreads", 200)));
		Configuration.ClassList classlist = Configuration.ClassList.setServerDefault(server);
		classlist.addBefore(JettyWebXmlConfiguration.class.getName(), AnnotationConfiguration.class.getName());
		WebAppContext context = new WebAppContext(null, new ConstraintSecurityHandler(), new ServletHandler(),
				new ErrorPageErrorHandler());
		String webInfoJarPattern = System.getProperty(WebInfConfiguration.WEBINF_JAR_PATTERN);
		if (webInfoJarPattern == null)
			webInfoJarPattern = ".*/ironrhino-[^/]*\\.jar$";
		if (!webInfoJarPattern.isEmpty())
			context.setAttribute(WebInfConfiguration.WEBINF_JAR_PATTERN, webInfoJarPattern);
		context.setContextPath("/");
		context.setInitParameter(DefaultServlet.CONTEXT_INIT + "dirAllowed", "false");
		String webappDir = System.getProperty("webapp.dir");
		if (webappDir != null) {
			context.setWar(webappDir);
			System.out.println("Webapp - " + webappDir);
		} else if (warUrl != null) {
			context.setWar(warUrl.toExternalForm());
			System.out.println("War - " + warUrl.getPath());
			System.setProperty("executable-war", warUrl.getPath());
		}
		context.setTempDirectory(tempDir);
		context.addServlet(context.addServlet(Default404Servlet.class, "*.class"), "/BOOT-INF/*");
		context.setServer(server);
		StatisticsHandler statisticsHandler = new StatisticsHandler();
		statisticsHandler.setHandler(context);
		server.setHandler(statisticsHandler);
		server.setStopAtShutdown(true);
		server.start();
		server.join();
	}

	private static File getTempDirectory(int port) {
		return new File(new File(System.getProperty("user.home")), ".jetty" + port);
	}

	private static boolean isAvailable(int port) {
		try (ServerSocket ignored = new ServerSocket(port)) {
			return true;
		} catch (IOException e) {
			return false;
		}
	}

}