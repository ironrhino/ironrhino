package org.ironrhino.core.servlet;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.AppInfo.Stage;
import org.ironrhino.core.util.JsonDesensitizer;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.springframework.util.FastByteArrayOutputStream;
import org.springframework.web.util.WebUtils;

/**
 * @See org.springframework.web.util.ContentCachingResponseWrapper
 */
public class LoggingBodyHttpServletResponse extends HttpServletResponseWrapper {

	private final Logger logger;

	private volatile ServletOutputStream streamOutputStream;

	private volatile PrintWriter writer;

	private final FastByteArrayOutputStream cachedContent = new FastByteArrayOutputStream();

	public LoggingBodyHttpServletResponse(HttpServletResponse response, Logger logger) {
		super(response);
		this.logger = logger;
	}

	@Override
	public String getCharacterEncoding() {
		String enc = super.getCharacterEncoding();
		return (enc != null ? enc : WebUtils.DEFAULT_CHARACTER_ENCODING);
	}

	@Override
	public void setContentLength(int len) {
		super.setContentLength(len);
		if (len > this.cachedContent.size()) {
			this.cachedContent.resize(len);
		}
	}

	@Override
	public void setContentLengthLong(long len) {
		super.setContentLengthLong(len);
		if (len > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Content-Length exceeds ContentCachingResponseWrapper's maximum ("
					+ Integer.MAX_VALUE + "): " + len);
		}
		int lenInt = (int) len;
		if (lenInt > this.cachedContent.size()) {
			this.cachedContent.resize(lenInt);
		}
	}

	@Override
	public void setBufferSize(int size) {
		super.setBufferSize(size);
		if (size > this.cachedContent.size()) {
			this.cachedContent.resize(size);
		}
	}

	@Override
	public void resetBuffer() {
		super.resetBuffer();
		this.cachedContent.reset();
	}

	@Override
	public void reset() {
		super.reset();
		this.cachedContent.reset();
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		ServletOutputStream temp = streamOutputStream;
		if (temp == null) {
			synchronized (this) {
				if ((temp = streamOutputStream) == null)
					streamOutputStream = temp = new ResponseServletOutputStream(getResponse(), logger, cachedContent);
			}
		}
		return temp;
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		if (this.writer == null) {
			this.writer = new ResponsePrintWriter(getOutputStream(), getCharacterEncoding());
		}
		return this.writer;
	}

	private static class ResponseServletOutputStream extends ServletOutputStream {

		private final Logger logger;

		private final ServletOutputStream os;

		private final String characterEncoding;

		private FastByteArrayOutputStream cachedContent;

		public ResponseServletOutputStream(ServletResponse response, Logger logger,
				FastByteArrayOutputStream cachedContent) throws IOException {
			this.logger = logger;
			this.os = response.getOutputStream();
			this.characterEncoding = response.getCharacterEncoding();
			this.cachedContent = cachedContent;
		}

		@Override
		public void write(int b) throws IOException {
			this.os.write(b);
			cachedContent.write(b);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			this.os.write(b, off, len);
			cachedContent.write(b, off, len);
		}

		@Override
		public void write(byte[] b) throws IOException {
			this.os.write(b);
			cachedContent.write(b);
		}

		@Override
		public boolean isReady() {
			return this.os.isReady();
		}

		@Override
		public void setWriteListener(WriteListener writeListener) {
			this.os.setWriteListener(writeListener);
		}

		@Override
		public void close() throws IOException {
			try {
				this.os.close();
			} finally {
				if (cachedContent != null) {
					byte[] bytes = cachedContent.toByteArray();
					cachedContent = null;
					if (bytes.length > 0) {
						String str = new String(bytes, 0, bytes.length, characterEncoding);
						if (AppInfo.getStage() != Stage.DEVELOPMENT)
							str = JsonDesensitizer.DEFAULT_INSTANCE.desensitize(str);
						String method = MDC.get("method");
						String url = MDC.get("url");
						MDC.remove("method");
						MDC.remove("url");
						logger.info("\n{}", str);
						MDC.put("method", method);
						MDC.put("url", url);
					}
				}
			}
		}
	}

	private static class ResponsePrintWriter extends PrintWriter {

		public ResponsePrintWriter(ServletOutputStream os, String characterEncoding) throws IOException {
			super(new OutputStreamWriter(os, characterEncoding));
		}

		@Override
		public void write(char[] buf, int off, int len) {
			super.write(buf, off, len);
			super.flush();
		}

		@Override
		public void write(String s, int off, int len) {
			super.write(s, off, len);
			super.flush();
		}

		@Override
		public void write(char[] buf) {
			super.write(buf);
			super.flush();
		}

		@Override
		public void write(String s) {
			super.write(s);
			super.flush();
		}

		@Override
		public void write(int c) {
			super.write(c);
			super.flush();
		}
	}

}