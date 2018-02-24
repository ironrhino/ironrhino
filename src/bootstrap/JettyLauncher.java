package bootstrap;

import java.io.File;
import java.net.URL;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.SessionIdManager;
import org.eclipse.jetty.server.session.HouseKeeper;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletHandler.Default404Servlet;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;

public class JettyLauncher {

	public static void start(URL warUrl) throws Exception {
		int port = 8080;
		String p = System.getProperty("port.http");
		if (p == null)
			p = System.getProperty("jetty.http.port");
		if (p != null && p.trim().length() > 0)
			port = Integer.valueOf(p);
		Server server = new Server(port);
		server.setSessionIdManager(new DummySessionIdManager());
		Configuration.ClassList classlist = Configuration.ClassList.setServerDefault(server);
		classlist.addBefore("org.eclipse.jetty.webapp.JettyWebXmlConfiguration",
				"org.eclipse.jetty.annotations.AnnotationConfiguration");
		WebAppContext context = new WebAppContext();
		context.setContextPath("/");
		context.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false");
		String webappDir = System.getProperty("webapp.dir");
		if (webappDir != null) {
			context.setWar(webappDir);
			System.out.println("Webapp - " + webappDir);
		} else if (warUrl != null) {
			context.setWar(warUrl.toExternalForm());
			System.out.println("War - " + warUrl.getPath());
			System.setProperty("executable-war", warUrl.getPath());
		}
		File tempDir = new File(new File(System.getProperty("user.home")), ".jetty" + port);
		tempDir.mkdirs();
		context.setTempDirectory(tempDir);
		context.setServer(server);
		context.addServlet(Default404Servlet.class, "*.class");

		server.setHandler(context);
		server.setStopAtShutdown(true);
		server.start();
		server.join();
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