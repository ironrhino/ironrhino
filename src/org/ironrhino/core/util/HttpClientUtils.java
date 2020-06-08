package org.ironrhino.core.util;

import java.io.IOException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.NoHttpResponseException;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.ironrhino.core.tracing.Tracing;

import io.opentracing.contrib.apache.http.client.TracingHttpClientBuilder;
import lombok.experimental.UtilityClass;

@UtilityClass
public class HttpClientUtils {

	private static Set<Header> DEFAULT_HEADERS = new HashSet<>();

	static {
		DEFAULT_HEADERS.add(new BasicHeader("User-Agent",
				"Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.138 Safari/537.36"));
		DEFAULT_HEADERS.add(new BasicHeader("Accept",
				"text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"));
		DEFAULT_HEADERS.add(new BasicHeader("Accept-Encoding", "gzip,deflate,sdch"));
	}

	static class HttpClientHolder {
		static CloseableHttpClient httpClient = create(5000, 10000, true);
	}

	public static CloseableHttpClient getDefaultInstance() {
		return HttpClientHolder.httpClient;
	}

	public static CloseableHttpClient create() {
		return create(5000, 10000);
	}

	public static CloseableHttpClient create(int connectTimeout, int socketTimeout) {
		return create(connectTimeout, socketTimeout, false);
	}

	public static CloseableHttpClient create(int connectTimeout, int socketTimeout, boolean evictExpiredConnections) {
		RequestConfig requestConfig = RequestConfig.custom().setConnectionRequestTimeout(connectTimeout)
				.setConnectTimeout(connectTimeout).setSocketTimeout(socketTimeout).build();
		HttpClientBuilder builder;
		if (Tracing.isEnabled()) {
			builder = new TracingHttpClientBuilder().disableCookieManagement();
			// call arbitrary method to cheat for compiler linking
			// avoid NoClassDefFoundError
		} else {
			builder = HttpClients.custom();
		}
		builder.disableAuthCaching().disableConnectionState().disableCookieManagement()
				.setConnectionTimeToLive(60, TimeUnit.SECONDS).setDefaultRequestConfig(requestConfig)
				.setDefaultHeaders(DEFAULT_HEADERS).useSystemProperties()
				.setRetryHandler((e, executionCount, httpCtx) -> executionCount < 3
						&& (e instanceof NoHttpResponseException || e instanceof UnknownHostException));
		if (evictExpiredConnections)
			builder.evictExpiredConnections();
		return builder.build();
	}

	public static String getResponseText(String url) throws IOException {
		return getResponseText(url, null, "UTF-8");
	}

	public static String getResponseText(String url, Map<String, String> params) throws IOException {
		return getResponseText(url, params, "UTF-8");
	}

	public static String getResponseText(String url, Map<String, String> params, Map<String, String> headers)
			throws IOException {
		return getResponseText(url, params, headers, "UTF-8");
	}

	public static String getResponseText(String url, Map<String, String> params, String charset) throws IOException {
		return getResponseText(url, params, null, charset);
	}

	public static String getResponseText(String url, Map<String, String> params, Map<String, String> headers,
			String charset) throws IOException {
		HttpGet httpRequest = null;
		StringBuilder sb = new StringBuilder(url);
		if (params != null && params.size() > 0) {
			sb.append(url.indexOf('?') < 0 ? '?' : '&');
			for (Map.Entry<String, String> entry : params.entrySet()) {
				sb.append(entry.getKey()).append("=").append(URLEncoder.encode(entry.getValue(), charset)).append("&");
			}
			sb.deleteCharAt(sb.length() - 1);
		}
		httpRequest = new HttpGet(sb.toString());
		if (headers != null && headers.size() > 0)
			for (Map.Entry<String, String> entry : headers.entrySet())
				httpRequest.addHeader(entry.getKey(), entry.getValue());
		return getDefaultInstance().execute(httpRequest, new BasicResponseHandler(charset));
	}

	public static String postResponseText(String url, Map<String, String> params) throws IOException {
		return postResponseText(url, params, "UTF-8");
	}

	public static String postResponseText(String url, Map<String, String> params, Map<String, String> headers)
			throws IOException {
		return postResponseText(url, params, headers, "UTF-8");
	}

	public static String postResponseText(String url, Map<String, String> params, String charset) throws IOException {
		return postResponseText(url, params, null, charset);
	}

	public static String postResponseText(String url, Map<String, String> params, Map<String, String> headers,
			String charset) throws IOException {
		HttpPost httpRequest = new HttpPost(url);
		if (params != null && params.size() > 0) {
			List<NameValuePair> nvps = new ArrayList<>();
			for (Map.Entry<String, String> entry : params.entrySet())
				nvps.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
			httpRequest.setEntity(new UrlEncodedFormEntity(nvps, charset));
		}
		if (headers != null && headers.size() > 0)
			for (Map.Entry<String, String> entry : headers.entrySet())
				httpRequest.addHeader(entry.getKey(), entry.getValue());
		return getDefaultInstance().execute(httpRequest, new BasicResponseHandler(charset));
	}

	public static String postResponseText(String url, String body, Map<String, String> headers, String charset)
			throws IOException {
		HttpPost httpRequest = new HttpPost(url);
		httpRequest.setEntity(new StringEntity(body, charset));
		if (headers != null && headers.size() > 0)
			for (Map.Entry<String, String> entry : headers.entrySet())
				httpRequest.addHeader(entry.getKey(), entry.getValue());
		return getDefaultInstance().execute(httpRequest, new BasicResponseHandler(charset));
	}

	public static String post(String url, String entity) throws IOException {
		return invoke("POST", url, entity);
	}

	public static String post(String url, String entity, String charset) throws IOException {
		return invoke("POST", url, entity, charset);
	}

	public static String put(String url, String entity) throws IOException {
		return invoke("PUT", url, entity);
	}

	public static String put(String url, String entity, String charset) throws IOException {
		return invoke("PUT", url, entity, charset);
	}

	public static String delete(String url) throws IOException {
		return invoke("DELETE", url, null);
	}

	public static String get(String url) throws IOException {
		return invoke("GET", url, null);
	}

	private static String invoke(String method, String url, String entity) throws IOException {
		return invoke(method, url, entity, "UTF-8");
	}

	private static String invoke(String method, String url, String entity, String charset) throws IOException {
		HttpRequestBase httpRequest = null;
		if (method.equalsIgnoreCase("GET"))
			httpRequest = new HttpGet(url);
		else if (method.equalsIgnoreCase("POST"))
			httpRequest = new HttpPost(url);
		else if (method.equalsIgnoreCase("PUT"))
			httpRequest = new HttpPut(url);
		else if (method.equalsIgnoreCase("DELETE"))
			httpRequest = new HttpDelete(url);
		if (entity != null)
			((HttpEntityEnclosingRequestBase) httpRequest).setEntity(new StringEntity(entity, charset));
		return getDefaultInstance().execute(httpRequest, new BasicResponseHandler(charset));
	}

	static class BasicResponseHandler implements ResponseHandler<String> {

		private final String charset;

		BasicResponseHandler(String charset) {
			this.charset = charset;
		}

		@Override
		public String handleResponse(HttpResponse response) throws HttpResponseException, IOException {
			try {
				StatusLine statusLine = response.getStatusLine();
				HttpEntity entity = response.getEntity();
				if (statusLine.getStatusCode() >= 300) {
					EntityUtils.consume(entity);
					throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
				}
				return entity == null ? null : EntityUtils.toString(entity, charset);
			} finally {
				((CloseableHttpResponse) response).close();
			}
		}

	}

}
