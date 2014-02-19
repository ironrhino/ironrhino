package org.ironrhino.core.servlet;

import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.commons.lang3.StringUtils;

public class LazyCommitResponseWrapper extends HttpServletResponseWrapper {

	private Buffer buffer;

	private String location;

	private int status;

	private String message;

	public LazyCommitResponseWrapper(final HttpServletResponse response) {
		super(response);
		buffer = new Buffer(getCharacterEncoding());
	}

	@Override
	public ServletOutputStream getOutputStream() {
		return buffer.getOutputStream();
	}

	@Override
	public PrintWriter getWriter() {
		return buffer.getWriter();
	}

	@Override
	public void setStatus(int sc) {
		this.status = sc;
	}

	@Override
	public void sendError(int sc) throws IOException {
		this.status = sc;
		this.message = "";
	}

	@Override
	public void sendError(int sc, String msg) throws IOException {
		this.status = sc;
		this.message = msg != null ? msg : "";
	}

	@Override
	public void sendRedirect(String location) throws IOException {
		this.location = location;
	}

	public byte[] getContents() throws IOException {
		if (buffer == null) {
			return null;
		} else {
			return buffer.getBytes();
		}
	}

	@Override
	public void flushBuffer() throws IOException {

	}

	public void commit() throws IOException {
		if (status > 0)
			super.setStatus(status);
		if (message != null) {
			super.sendError(status, message);
		} else if (location != null) {
			super.sendRedirect(location);
		} else {
			byte[] bytes = getContents();
			if (bytes == null || bytes.length == 0)
				return;
			setContentLength(bytes.length);
			ServletOutputStream sos = super.getOutputStream();
			sos.write(bytes);
			sos.flush();
			sos.close();
		}
	}

	private static class Buffer {

		public static final int SIZE = 5 * 1024;

		private String encoding;

		private CharArrayWriter bufferedWriter;

		private ByteArrayOutputStream bufferedStream;

		private PrintWriter exposedWriter;

		private ServletOutputStream exposedStream;

		public Buffer(String encoding) {
			if (StringUtils.isBlank(encoding))
				encoding = "UTF-8";
			this.encoding = encoding;
		}

		public byte[] getBytes() throws IOException {
			if (bufferedWriter != null)
				return bufferedWriter.toString().getBytes(encoding);
			else if (bufferedStream != null)
				return bufferedStream.toByteArray();
			else
				return new byte[0];
		}

		public PrintWriter getWriter() {
			if (bufferedWriter == null) {
				if (bufferedStream != null) {
					throw new IllegalStateException(
							"response.getWriter() called after response.getOutputStream()");
				}
				bufferedWriter = new CharArrayWriter(128);
				exposedWriter = new PrintWriter(bufferedWriter);
			}
			return exposedWriter;
		}

		public ServletOutputStream getOutputStream() {
			if (bufferedStream == null) {
				if (bufferedWriter != null) {
					throw new IllegalStateException(
							"response.getOutputStream() called after response.getWriter()");
				}
				bufferedStream = new ByteArrayOutputStream(SIZE);
				exposedStream = new ServletOutputStream() {

					@Override
					public void write(int b) {
						bufferedStream.write(b);
					}

					@Override
					public boolean isReady() {
						return true;
					}

					@Override
					public void setWriteListener(WriteListener listener) {
						
					}
				};
			}
			return exposedStream;
		}

	}

}