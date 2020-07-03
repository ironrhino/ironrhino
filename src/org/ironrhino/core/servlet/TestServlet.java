package org.ironrhino.core.servlet;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServlet;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.AppInfo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestServlet extends HttpServlet {

	private static final long serialVersionUID = 9128941579865103381L;

	@Override
	public void init() {
		final String url = getInitParameter("url");
		new Thread(() -> {
			try {
				TimeUnit.SECONDS.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (StringUtils.isNotBlank(url)) {
				if (test(url))
					log.info("test succussful");
				else
					log.warn("test failed, no response, please check it");
			} else {
				String context = getServletContext().getContextPath();
				String format = "http://localhost%s%s/_ping?_internal_testing_";
				int port = AppInfo.getHttpPort();
				if (port > 0 && port != 80) {
					if (test(String.format(format, ":" + port, context)))
						log.info("test succussful");
					else
						log.warn("test failed,no response,please check it");
				} else {
					if (test(String.format(format, "", context)))
						log.info("test succussful");
					else {
						if (test(String.format(format, ":8080", context)))
							log.info("test succussful");
						else
							log.warn("test failed, no response, please check it");
					}
				}
			}
		}, "test").start();
	}

	private boolean test(String testurl) {
		log.info("testing: " + testurl);
		try {
			HttpURLConnection conn = (HttpURLConnection) new URL(testurl).openConnection();
			conn.setConnectTimeout(1000);
			conn.setReadTimeout(1000);
			conn.connect();
			int status = conn.getResponseCode();
			conn.disconnect();
			return status == 200;
		} catch (Exception e) {
			return false;
		}
	}
}
