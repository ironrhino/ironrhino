package bootstrap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Main {
	private final String[] jettyJars = { "WEB-INF/libext/jetty.jar",
			"WEB-INF/libext/servlet-api.jar",
			"WEB-INF/libext/websocket-api.jar", "WEB-INF/libext/bootstrap.jar" };

	public static void main(String[] args) throws Exception {
		String javaVersion = System.getProperty("java.version");
		StringTokenizer tokens = new StringTokenizer(javaVersion, ".-_");
		int majorVersion = Integer.parseInt(tokens.nextToken());
		int minorVersion = Integer.parseInt(tokens.nextToken());
		if ((majorVersion < 2) && (minorVersion < 7)) {
			System.err.println("requires Java 7 or later.");
			System.err.println("Your java version is " + javaVersion);
			System.err
					.println("Java Home:  " + System.getProperty("java.home"));
			System.exit(0);
		}
		Main main = new Main();
		main.launchJetty();
	}

	private void launchJetty() throws Exception {
		ProtectionDomain protectionDomain = Main.class.getProtectionDomain();
		URL warUrl = protectionDomain.getCodeSource().getLocation();
		System.out.println(warUrl.getPath());
		List<URL> jarUrls = extractJettyJarsFromWar(warUrl.getPath());
		ClassLoader urlClassLoader = new URLClassLoader(
				(URL[]) jarUrls.toArray(new URL[jarUrls.size()]));
		Thread.currentThread().setContextClassLoader(urlClassLoader);
		Class<?> jettyUtil = urlClassLoader
				.loadClass("bootstrap.JettyLauncher");
		Method mainMethod = jettyUtil.getMethod("start",
				new Class[] { URL.class });
		mainMethod.invoke(null, new Object[] { warUrl });
	}

	private List<URL> extractJettyJarsFromWar(String warPath)
			throws IOException {
		try (JarFile jarFile = new JarFile(warPath)) {
			List<URL> jarUrls = new ArrayList<URL>();
			for (String entryPath : this.jettyJars) {
				File tmpFile;
				try {
					tmpFile = File.createTempFile(
							entryPath.replaceAll("/", "_"), "war");
				} catch (IOException e) {
					String tmpdir = System.getProperty("java.io.tmpdir");
					throw new IOException("Failed to extract " + entryPath
							+ " to " + tmpdir, e);
				}
				JarEntry jarEntry = jarFile.getJarEntry(entryPath);
				try (InputStream inStream = jarFile.getInputStream(jarEntry)) {
					OutputStream outStream = new FileOutputStream(tmpFile);
					try {
						byte[] buffer = new byte[8192];
						int readLength;
						while ((readLength = inStream.read(buffer)) > 0)
							outStream.write(buffer, 0, readLength);
					} catch (Exception exc) {
						exc.printStackTrace();
					} finally {
						outStream.close();
					}

					tmpFile.deleteOnExit();

					jarUrls.add(tmpFile.toURI().toURL());
				}
			}
			return jarUrls;
		}
	}
}