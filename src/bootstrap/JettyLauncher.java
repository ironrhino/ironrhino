package bootstrap;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URL;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.SessionIdManager;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.server.session.HouseKeeper;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletHandler.Default404Servlet;
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
			port = Integer.valueOf(p);
		if (port == -1) {
			port = 8080;
			while (getTempDirectory(port).exists() || !available(port))
				port++;
			System.setProperty("port.http", String.valueOf(port));
		}
		File tempDir = getTempDirectory(port);
		tempDir.mkdirs();
		Server server = new Server(port);
		server.setSessionIdManager(new DummySessionIdManager());
		Configuration.ClassList classlist = Configuration.ClassList.setServerDefault(server);
		classlist.addBefore(JettyWebXmlConfiguration.class.getName(), AnnotationConfiguration.class.getName());
		WebAppContext context = new WebAppContext();
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
		context.setServer(server);
		context.addServlet(Default404Servlet.class, "*.class");

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

	private static boolean available(int port) {
		ServerSocket ss = null;
		try {
			ss = new ServerSocket(port);
			ss.setReuseAddress(true);
			return true;
		} catch (IOException e) {
		} finally {
			if (ss != null) {
				try {
					ss.close();
				} catch (IOException e) {
					/* should not be thrown */
				}
			}
		}
		return false;
	}

	static class DummySessionIdManager implements SessionIdManager {

		@Override
		public void start() throws Exception {
		}

		@Override
		public void stop() throws Exception {
		}

		@Override
		public boolean isRunning() {
			return false;
		}

		@Override
		public boolean isStarted() {
			return false;
		}

		@Override
		public boolean isStarting() {
			return false;
		}

		@Override
		public boolean isStopping() {
			return false;
		}

		@Override
		public boolean isStopped() {
			return false;
		}

		@Override
		public boolean isFailed() {
			return false;
		}

		@Override
		public void addLifeCycleListener(Listener listener) {
		}

		@Override
		public void removeLifeCycleListener(Listener listener) {
		}

		@Override
		public boolean isIdInUse(String id) {
			return false;
		}

		@Override
		public void expireAll(String id) {
		}

		@Override
		public void invalidateAll(String id) {
		}

		@Override
		public String newSessionId(HttpServletRequest request, long created) {
			return null;
		}

		@Override
		public String getWorkerName() {
			return null;
		}

		@Override
		public String getId(String extendedId) {
			return null;
		}

		@Override
		public String getExtendedId(String clusterId, HttpServletRequest request) {
			return null;
		}

		@Override
		public String renewSessionId(String oldClusterId, String oldNodeId, HttpServletRequest request) {
			return null;
		}

		@Override
		public Set<SessionHandler> getSessionHandlers() {
			return null;
		}

		@Override
		public void setSessionHouseKeeper(HouseKeeper houseKeeper) {

		}

		@Override
		public HouseKeeper getSessionHouseKeeper() {
			return null;
		}

	}

}