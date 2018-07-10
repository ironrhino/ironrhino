package org.ironrhino.core.servlet;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;
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

	private PrintWriter writer;

	private final FastByteArrayOutputStream content = new FastByteArrayOutputStream(1024);

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
		if (len > this.content.size()) {
			this.content.resize(len);
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
		if (lenInt > this.content.size()) {
			this.content.resize(lenInt);
		}
	}

	@Override
	public void setBufferSize(int size) {
		super.setBufferSize(size);
		if (size > this.content.size()) {
			this.content.resize(size);
		}
	}

	@Override
	public void resetBuffer() {
		super.resetBuffer();
		this.content.reset();
	}

	@Override
	public void reset() {
		super.reset();
		this.content.reset();
	}

	public byte[] getContentAsByteArray() {
		return this.content.toByteArray();
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		ServletOutputStream temp = streamOutputStream;
		if (temp == null) {
			synchronized (this) {
				temp = streamOutputStream;
				if (temp == null)
					streamOutputStream = temp = new ResponseServletOutputStream(super.getOutputStream());
			}
		}
		return temp;
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		if (this.writer == null) {
			this.writer = new ResponsePrintWriter(getCharacterEncoding());
		}
		return this.writer;
	}

	private class ResponseServletOutputStream extends ServletOutputStream {

		private final ServletOutputStream os;

		public ResponseServletOutputStream(ServletOutputStream os) {
			this.os = os;
		}

		@Override
		public void write(int b) throws IOException {
			this.os.write(b);
			content.write(b);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			this.os.write(b, off, len);
			content.write(b, off, len);
		}

		@Override
		public void write(byte[] b) throws IOException {
			this.os.write(b);
			content.write(b);
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
			this.os.close();
			byte[] bytes = getContentAsByteArray();
			if (bytes.length > 0) {
				String encoding = getCharacterEncoding();
				MDC.remove("method");
				MDC.remove("url");
				String str = new String(bytes, 0, bytes.length, encoding);
				if (AppInfo.getStage() != Stage.DEVELOPMENT)
					str = JsonDesensitizer.DEFAULT_INSTANCE.desensitize(str);
				logger.info("\n{}", str);
			}
		}
	}

	private class ResponsePrintWriter extends PrintWriter {

		public ResponsePrintWriter(String characterEncoding) throws IOException {
			super(new OutputStreamWriter(getOutputStream(), characterEncoding));
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