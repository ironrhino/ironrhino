package org.ironrhino.core.servlet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.HttpClientUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestServlet extends HttpServlet {

	private static final long serialVersionUID = 9128941579865103381L;
	private Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public void init() {
		final String url = getInitParameter("url");
		new Thread() {
			@Override
			public void run() {
				if (StringUtils.isNotBlank(url)) {
					if (test(url))
						logger.info("test succussful");
					else
						logger.warn("test failed,no response,please check it");
				} else {
					String contextPath = getServletContext().getContextPath();
					String format = "http://localhost%s%s";
					int port = AppInfo.getHttpPort();
					String context = (contextPath.indexOf('/') == 0 ? "" : "/")
							+ contextPath;
					if (port > 0 && port != 80) {
						if (test(String.format(format, ":" + port, context)))
							logger.info("test succussful");
					} else {
						if (test(String.format(format, "", context)))
							logger.info("test succussful");
						else {
							if (test(String.format(format, ":8080", context)))
								logger.info("test succussful");
							else
								logger.warn("test failed,no response,please check it");
						}
					}
				}
			}
		}.start();
	}

	private boolean test(String testurl) {
		logger.info("testing: " + testurl);
		HttpRequestBase httpRequest = new HttpGet(testurl);
		try {
			return HttpClientUtils.getDefaultInstance().execute(httpRequest)
					.getStatusLine().getStatusCode() == HttpServletResponse.SC_OK;
		} catch (Exception e) {
			httpRequest.abort();
			return false;
		}
	}
}
