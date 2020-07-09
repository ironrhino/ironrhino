package org.ironrhino.core.spring.http.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.AppInfo.Stage;
import org.ironrhino.core.util.JsonDesensitizer;
import org.slf4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoggingClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException {
		MediaType contentType = request.getHeaders().getContentType();
		if (body != null && body.length > 0 && contentType != null && supports(contentType)) {
			String str = new String(body, StandardCharsets.UTF_8);
			if (AppInfo.getStage() != Stage.DEVELOPMENT)
				str = JsonDesensitizer.DEFAULT_INSTANCE.desensitize(str);
			log.info("{} {} \n{}", request.getMethod(), request.getURI(), str);
		} else {
			log.info("{} {}", request.getMethod(), request.getURI());
		}
		ClientHttpResponse response = execution.execute(request, body);
		if (!response.getStatusCode().isError()) {
			contentType = response.getHeaders().getContentType();
			if (supports(contentType)) {
				return new ClientHttpResponse() {

					InputStream is;

					@Override
					public InputStream getBody() throws IOException {
						if (is == null)
							is = new ContentCachingInputStream(response, log);
						return is;
					}

					@Override
					public HttpHeaders getHeaders() {
						return response.getHeaders();
					}

					@Override
					public HttpStatus getStatusCode() throws IOException {
						return response.getStatusCode();
					}

					@Override
					public int getRawStatusCode() throws IOException {
						return response.getRawStatusCode();
					}

					@Override
					public String getStatusText() throws IOException {
						return response.getStatusText();
					}

					@Override
					public void close() {
						if (is != null)
							try {
								is.close();
								is = null;
							} catch (IOException e) {
								log.error(e.getMessage(), e);
							}
						response.close();
					}

				};

			}
		}
		return response;
	}

	protected boolean supports(MediaType contentType) {
		if (contentType == null)
			return false;
		return contentType.isCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED)
				|| contentType.isCompatibleWith(MediaType.APPLICATION_JSON)
				|| contentType.isCompatibleWith(MediaType.APPLICATION_XML) || contentType.getType().equals("text");
	}

	private class ContentCachingInputStream extends InputStream {

		private final InputStream is;

		private final Logger logger;

		private ByteArrayOutputStream cachedContent;

		public ContentCachingInputStream(ClientHttpResponse response, Logger logger) throws IOException {
			this.is = response.getBody();
			int contentLength = (int) response.getHeaders().getContentLength();
			this.cachedContent = new ByteArrayOutputStream(contentLength >= 0 ? contentLength : 1024);
			this.logger = logger;
		}

		@Override
		public int read(byte[] b) throws IOException {
			int count = super.read(b);
			cache(b, 0, count);
			return count;
		}

		@Override
		public int read(byte[] b, final int off, final int len) throws IOException {
			int count = is.read(b, off, len);
			cache(b, off, count);
			return count;
		}

		@Override
		public int read() throws IOException {
			int ch = this.is.read();
			if (ch != -1)
				cachedContent.write(ch);
			return ch;
		}

		private void cache(byte[] b, final int off, final int count) {
			if (count != -1)
				cachedContent.write(b, off, count);
		}

		@Override
		public void close() throws IOException {
			this.is.close();
			if (cachedContent != null) {
				byte[] bytes = cachedContent.toByteArray();
				cachedContent = null;
				String str = new String(bytes, StandardCharsets.UTF_8);
				if (AppInfo.getStage() != Stage.DEVELOPMENT)
					str = JsonDesensitizer.DEFAULT_INSTANCE.desensitize(str);
				logger.info("Received:\n{}", str);
			}
		}
	}
}
