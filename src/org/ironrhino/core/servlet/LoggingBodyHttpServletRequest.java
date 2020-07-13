package org.ironrhino.core.servlet;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.AppInfo.Stage;
import org.ironrhino.core.util.JsonDesensitizer;
import org.slf4j.Logger;
import org.springframework.web.util.WebUtils;

/**
 * @See org.springframework.web.util.ContentCachingRequestWrapper
 */
public class LoggingBodyHttpServletRequest extends HttpServletRequestWrapper {

	private volatile ServletInputStream servletInputStream;

	private volatile BufferedReader reader;

	private final Logger logger;

	private final ByteArrayOutputStream cachedContent;

	public LoggingBodyHttpServletRequest(HttpServletRequest request, Logger logger) {
		super(request);
		int contentLength = request.getContentLength();
		this.cachedContent = new ByteArrayOutputStream(contentLength >= 0 ? contentLength : 1024);
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
				if ((temp = servletInputStream) == null)
					servletInputStream = temp = new ContentCachingInputStream(super.getInputStream());
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

	public byte[] getContentAsByteArray() {
		return this.cachedContent.toByteArray();
	}

	private class ContentCachingInputStream extends ServletInputStream {

		private final ServletInputStream is;

		public ContentCachingInputStream(ServletInputStream is) {
			this.is = is;
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

		private void cache(byte[] b, final int off, final int count) {
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
			this.is.close();
			byte[] bytes = getContentAsByteArray();
			String str = new String(bytes, 0, bytes.length, getCharacterEncoding());
			if (AppInfo.getStage() != Stage.DEVELOPMENT)
				str = JsonDesensitizer.DEFAULT_INSTANCE.desensitize(str);
			logger.info("\n{}", str);
		}
	}

}