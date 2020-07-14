package org.ironrhino.core.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.AppInfo.Stage;
import org.ironrhino.core.util.JsonDesensitizer;
import org.slf4j.Logger;
import org.springframework.util.FastByteArrayOutputStream;
import org.springframework.web.util.WebUtils;

/**
 * @See org.springframework.web.util.ContentCachingRequestWrapper
 */
public class LoggingBodyHttpServletRequest extends HttpServletRequestWrapper {

	private final Logger logger;

	private volatile ServletInputStream servletInputStream;

	private volatile BufferedReader reader;

	public LoggingBodyHttpServletRequest(HttpServletRequest request, Logger logger) {
		super(request);
		this.logger = logger;
	}

	@Override
	public String getCharacterEncoding() {
		String enc = super.getCharacterEncoding();
		return (enc != null ? enc : WebUtils.DEFAULT_CHARACTER_ENCODING);
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		ServletInputStream temp = servletInputStream;
		if (temp == null) {
			synchronized (this) {
				if ((temp = servletInputStream) == null) {
					servletInputStream = temp = new ContentCachingInputStream(getRequest(), logger);
				}
			}
		}
		return temp;
	}

	@Override
	public BufferedReader getReader() throws IOException {
		if (this.reader == null) {
			this.reader = new BufferedReader(new InputStreamReader(getInputStream(), getCharacterEncoding()));
		}
		return this.reader;
	}

	private static class ContentCachingInputStream extends ServletInputStream {

		private final Logger logger;

		private final ServletInputStream is;

		private final String characterEncoding;

		private FastByteArrayOutputStream cachedContent;

		public ContentCachingInputStream(ServletRequest request, Logger logger) throws IOException {
			this.logger = logger;
			this.is = request.getInputStream();
			this.characterEncoding = request.getCharacterEncoding();
			int contentLength = request.getContentLength();
			this.cachedContent = new FastByteArrayOutputStream(contentLength >= 0 ? contentLength : 1024);

		}

		@Override
		public int readLine(byte[] b, final int off, final int len) throws IOException {
			int count = is.readLine(b, off, len);
			cache(b, off, count);
			return count;
		}

		@Override
		public int read(byte[] b, final int off, final int len) throws IOException {
			int count = is.read(b, off, len);
			cache(b, off, count);
			return count;
		}

		private void cache(byte[] b, final int off, final int count) throws IOException {
			if (count != -1)
				cachedContent.write(b, off, count);
		}

		@Override
		public int read() throws IOException {
			int ch = this.is.read();
			if (ch != -1)
				cachedContent.write(ch);
			return ch;
		}

		@Override
		public boolean isFinished() {
			return this.is.isFinished();
		}

		@Override
		public boolean isReady() {
			return this.is.isReady();
		}

		@Override
		public void setReadListener(ReadListener readListener) {
			this.is.setReadListener(readListener);
		}

		@Override
		public void close() throws IOException {
			try {
				this.is.close();
			} finally {
				if (cachedContent != null) {
					byte[] bytes = cachedContent.toByteArray();
					cachedContent = null;
					String str = new String(bytes, 0, bytes.length, characterEncoding);
					if (AppInfo.getStage() != Stage.DEVELOPMENT)
						str = JsonDesensitizer.DEFAULT_INSTANCE.desensitize(str);
					logger.info("\n{}", str);
				}
			}
		}
	}

}