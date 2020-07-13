package org.ironrhino.core.spring.http.client;

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
import org.springframework.util.FastByteArrayOutputStream;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class LoggingClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

	private final Logger logger;

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException {
		MediaType contentType = request.getHeaders().getContentType();
		if (body.length > 0 && contentType != null && supports(contentType)) {
			String str = new String(body, StandardCharsets.UTF_8);
			if (AppInfo.getStage() != Stage.DEVELOPMENT && contentType.isCompatibleWith(MediaType.APPLICATION_JSON))
				str = JsonDesensitizer.DEFAULT_INSTANCE.desensitize(str);
			logger.info("{} {} \n{}", request.getMethod(), request.getURI(), str);
		} else {
			logger.info("{} {}", request.getMethod(), request.getURI());
		}
		ClientHttpResponse response = execution.execute(request, body);
		contentType = response.getHeaders().getContentType();
		if (response.getHeaders().getContentLength() != 0 && supports(contentType)) {
			return new ClientHttpResponse() {

				InputStream is;

				@Override
				public InputStream getBody() throws IOException {
					if (is == null)
						is = new ContentCachingInputStream(response, logger);
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
							logger.error(e.getMessage(), e);
						}
					response.close();
				}

			};
		} else {
			logger.info("Received status {} and content type \"{}\" with length {}", response.getRawStatusCode(),
					contentType, response.getHeaders().getContentLength());
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

	private static class ContentCachingInputStream extends InputStream {

		private final InputStream is;

		private final MediaType contentType;

		private final Logger logger;

		private FastByteArrayOutputStream cachedContent;

		public ContentCachingInputStream(ClientHttpResponse response, Logger logger) throws IOException {
			this.is = response.getBody();
			this.contentType = response.getHeaders().getContentType();
			int contentLength = (int) response.getHeaders().getContentLength();
			this.cachedContent = new FastByteArrayOutputStream(contentLength >= 0 ? contentLength : 1024);
			this.logger = logger;
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

		private void cache(byte[] b, final int off, final int count) throws IOException {
			if (count != -1)
				cachedContent.write(b, off, count);
		}

		@Override
		public void close() throws IOException {
			try {
				this.is.close();
			} finally {
				if (cachedContent != null) {
					byte[] bytes = cachedContent.toByteArray();
					cachedContent = null;
					String str = new String(bytes, StandardCharsets.UTF_8);
					if (AppInfo.getStage() != Stage.DEVELOPMENT
							&& contentType.isCompatibleWith(MediaType.APPLICATION_JSON))
						str = JsonDesensitizer.DEFAULT_INSTANCE.desensitize(str);
					logger.info("Received:\n{}", str);
				}
			}
		}
	}
}
