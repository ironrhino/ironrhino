package org.ironrhino.core.tracing;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import io.jaegertracing.internal.exceptions.SenderException;
import io.jaegertracing.thrift.internal.senders.ThriftSender;
import io.jaegertracing.thriftjava.Batch;
import io.jaegertracing.thriftjava.Process;
import io.jaegertracing.thriftjava.Span;

public class HttpSender extends ThriftSender {

	private static final String HTTP_COLLECTOR_JAEGER_THRIFT_FORMAT_PARAM = "format=jaeger.thrift";

	private static final ContentType CONTENT_TYPE_THRIFT = ContentType.create("application/x-thrift");

	private static final int CONNECTION_TIMEOUT = 5000;

	private static final int CONNECTION_TIME_TO_LIVE = 60;

	private final CloseableHttpClient httpClient;

	private final String collectorUrl;

	public HttpSender(String endpoint) {
		this(endpoint, 0);
	}

	public HttpSender(String endpoint, int maxPacketSize) {
		super(ProtocolType.Binary, maxPacketSize);
		if (endpoint == null)
			throw new IllegalArgumentException("endpoint should'nt be null");
		collectorUrl = String.format("%s?%s", endpoint, HTTP_COLLECTOR_JAEGER_THRIFT_FORMAT_PARAM);
		this.httpClient = HttpClients.custom().disableAuthCaching().disableAutomaticRetries().disableConnectionState()
				.disableCookieManagement().setConnectionTimeToLive(CONNECTION_TIME_TO_LIVE, TimeUnit.SECONDS)
				.setDefaultRequestConfig(RequestConfig.custom().setConnectTimeout(CONNECTION_TIMEOUT).build()).build();
	}

	@Override
	public void send(Process process, List<Span> spans) throws SenderException {
		Batch batch = new Batch(process, spans);
		byte[] bytes = null;
		try {
			bytes = serialize(batch);
		} catch (Exception e) {
			throw new SenderException(String.format("Failed to serialize %d spans", spans.size()), e, spans.size());
		}
		HttpPost post = new HttpPost(collectorUrl);
		post.setEntity(new ByteArrayEntity(bytes, CONTENT_TYPE_THRIFT));
		try {
			HttpResponse response = httpClient.execute(post);
			if (response.getStatusLine().getStatusCode() != 200) {
				String responseBody;
				try {
					responseBody = EntityUtils.toString(response.getEntity());
				} catch (IOException e) {
					responseBody = "unable to read response";
				}
				String exceptionMessage = String.format("Could not send %d spans, response %d: %s", spans.size(),
						response.getStatusLine().getStatusCode(), responseBody);
				throw new SenderException(exceptionMessage, null, spans.size());
			}
		} catch (IOException e) {
			throw new SenderException(String.format("Could not send %d spans", spans.size()), e, spans.size());
		}
	}

	@Override
	public int close() throws SenderException {
		int result = super.close();
		try {
			httpClient.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

}
