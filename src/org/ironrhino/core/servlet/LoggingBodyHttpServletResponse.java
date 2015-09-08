package org.ironrhino.core.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.slf4j.Logger;
import org.slf4j.MDC;

public class LoggingBodyHttpServletResponse extends HttpServletResponseWrapper {

	private Logger logger;

	private volatile ServletOutputStream streamOutputStream;

	private ByteArrayOutputStream baos = new ByteArrayOutputStream(2048);

	public LoggingBodyHttpServletResponse(HttpServletResponse response, Logger logger) {
		super(response);
		this.logger = logger;
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {

		final ServletOutputStream os = super.getOutputStream();
		if (streamOutputStream == null) {
			synchronized (this) {
				if (streamOutputStream == null) {
					streamOutputStream = new ServletOutputStream() {

						@Override
						public boolean isReady() {
							return os.isReady();
						}

						@Override
						public void setWriteListener(WriteListener writeListener) {
							os.setWriteListener(writeListener);
						}

						@Override
						public void write(int b) throws IOException {
							os.write(b);
							baos.write(b);
						}

						@Override
						public void flush() throws IOException {
							os.flush();
						}

						@Override
						public void close() throws IOException {
							os.flush();
							os.close();
							if (baos != null) {
								byte[] bytes = baos.toByteArray();
								baos.close();
								baos = null;
								String encoding = getCharacterEncoding();
								if (encoding == null)
									encoding = "UTF-8";
								MDC.remove("method");
								MDC.remove("url");
								logger.info("\n{}", new String(bytes, 0, bytes.length, encoding));
							}
						}

					};
				}
			}
		}
		return streamOutputStream;

	}

}