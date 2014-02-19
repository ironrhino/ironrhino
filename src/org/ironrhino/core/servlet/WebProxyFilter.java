package org.ironrhino.core.servlet;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.ironrhino.core.util.HttpClientUtils;

public class WebProxyFilter implements Filter {

	private CloseableHttpClient httpClient;

	private boolean checkSameOrigin = false;

	@Override
	public void destroy() {
		try {
			httpClient.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp,
			FilterChain chain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) resp;
		URL requestURL = new URL(request.getRequestURL().toString());
		if (checkSameOrigin) {
			String referer = request.getHeader("Referer");
			if (StringUtils.isBlank(referer))
				return;
			URL url = new URL(referer);
			if (!url.getProtocol().equalsIgnoreCase(requestURL.getProtocol())
					|| !url.getHost().equalsIgnoreCase(requestURL.getHost())
					|| url.getPort() != requestURL.getPort())
				return;
		}
		String uri = requestURL.toString();
		String target = null;
		int index = uri.indexOf("http://", uri.indexOf("://") + 1);
		if (index > 0)
			target = uri.substring(index);
		if (target == null) {
			index = uri.indexOf("https://", uri.indexOf("://") + 1);
			if (index > 0)
				target = uri.substring(index);
		}
		if (target == null)
			return;
		String queryString = request.getQueryString();
		StringBuilder sb = new StringBuilder();
		sb.append(target);
		if (StringUtils.isNotBlank(queryString)) {
			sb.append('?');
			sb.append(queryString);
		}
		uri = sb.toString();
		HttpRequestBase httpRequest;
		if ("GET".equalsIgnoreCase(request.getMethod())) {
			httpRequest = new HttpGet(uri);
		} else if ("POST".equalsIgnoreCase(request.getMethod())) {
			httpRequest = new HttpPost(uri);
		} else if ("PUT".equalsIgnoreCase(request.getMethod())) {
			httpRequest = new HttpPut(uri);
		} else if ("DELETE".equalsIgnoreCase(request.getMethod())) {
			httpRequest = new HttpDelete(uri);
		} else {
			httpRequest = new HttpGet(uri);
		}
		if (httpRequest instanceof HttpEntityEnclosingRequestBase) {
			Enumeration<String> en = request.getParameterNames();
			if (en.hasMoreElements()) {
				List<NameValuePair> nvps = new ArrayList<NameValuePair>();
				while (en.hasMoreElements()) {
					String name = en.nextElement();
					for (String value : request.getParameterValues(name))
						if (queryString == null
								|| !queryString.contains(name + "="))
							nvps.add(new BasicNameValuePair(name, value));
				}
				((HttpEntityEnclosingRequestBase) httpRequest)
						.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
			}
		}

		HttpEntity entity = null;
		try {
			HttpResponse rsp = httpClient.execute(httpRequest);

			StatusLine sl = rsp.getStatusLine();
			if (sl.getStatusCode() >= 300) {
				response.sendError(sl.getStatusCode(), sl.getReasonPhrase());
				httpRequest.abort();
				return;
			}

			entity = rsp.getEntity();
			if (entity != null) {
				for (Header h : httpRequest.getAllHeaders())
					response.setHeader(h.getName(), h.getValue());
				entity.writeTo(response.getOutputStream());
			}
		} catch (Exception e) {
			httpRequest.abort();
			e.printStackTrace();
		}
	}

	@Override
	public void init(FilterConfig config) throws ServletException {
		String s = config.getInitParameter("checkSameOrigin");
		if (StringUtils.isNotBlank(s))
			checkSameOrigin = Boolean.parseBoolean(s.trim());
		httpClient = HttpClientUtils.getDefaultInstance();
	}

}
