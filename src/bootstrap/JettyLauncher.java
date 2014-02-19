package bootstrap;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;

public class JettyLauncher {

	public static void start(URL warUrl) throws Exception {
		int port = 8080;
		String p = System.getProperty("port.http");
		if (p != null && p.trim().length() > 0)
			port = Integer.valueOf(p);
		Server server = new Server(port);
		WebAppContext context = new WebAppContext();
		File tempDir = new File(new File(System.getProperty("user.home")),
				".jetty");
		tempDir.mkdirs();
		context.setTempDirectory(tempDir);
		context.setContextPath("/");
		context.setServer(server);
		context.addServlet(NotFoundServlet.class.getName(), "*.class");
		context.setWar(warUrl.toExternalForm());
		System.out.println("War - " + warUrl.getPath());
		System.setProperty("executable-war", warUrl.getPath());
		server.setHandler(context);
		server.setStopAtShutdown(true);
		server.start();
		server.join();
	}

	public static class NotFoundServlet extends HttpServlet {
		private static final long serialVersionUID = 8492638656439246491L;

		@Override
		protected void doGet(HttpServletRequest request,
				HttpServletResponse response) throws ServletException,
				IOException {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}

}