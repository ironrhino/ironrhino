package bootstrap;

import java.io.File;
import java.net.URL;

import org.eclipse.jetty.server.Server;
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
		Configuration.ClassList classlist = Configuration.ClassList.setServerDefault(server);
		classlist.addBefore("org.eclipse.jetty.webapp.JettyWebXmlConfiguration",
				"org.eclipse.jetty.annotations.AnnotationConfiguration");
		WebAppContext context = new WebAppContext();
		context.setContextPath("/");
		context.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false");
		String webappDir = System.getProperty("webapp.dir");
		if (webappDir != null) {
			context.setWar(webappDir);
		} else {
			context.setWar(warUrl.toExternalForm());
		}
		File tempDir = new File(new File(System.getProperty("user.home")), ".jetty" + port);
		tempDir.mkdirs();
		context.setTempDirectory(tempDir);
		context.setServer(server);
		context.addServlet(Default404Servlet.class, "*.class");

		System.out.println("War - " + warUrl.getPath());
		System.setProperty("executable-war", warUrl.getPath());
		server.setHandler(context);
		server.setStopAtShutdown(true);
		server.start();
		server.join();
	}

}