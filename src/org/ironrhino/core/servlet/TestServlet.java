package org.ironrhino.core.servlet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.HttpClientUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestServlet extends HttpServlet {

	private static final long serialVersionUID = 9128941579865103381L;

	@Override
	public void init() {
		final String url = getInitParameter("url");
		new Thread(() -> {
			if (StringUtils.isNotBlank(url)) {
				if (test(url))
					log.info("test succussful");
				else
					log.warn("test failed,no response,please check it");
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
							log.warn("test failed,no response,please check it");
					}
				}
			}
		}).start();
	}

	private boolean test(String testurl) {
		log.info("testing: " + testurl);
		HttpRequestBase httpRequest = new HttpGet(testurl);
		try {
			return HttpClientUtils.getDefaultInstance().execute(httpRequest).getStatusLine()
					.getStatusCode() == HttpServletResponse.SC_OK;
		} catch (Exception e) {
			httpRequest.abort();
			return false;
		} finally {
			httpRequest.releaseConnection();
		}
	}
}
