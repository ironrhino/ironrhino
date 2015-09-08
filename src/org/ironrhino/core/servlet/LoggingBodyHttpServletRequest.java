package org.ironrhino.core.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.slf4j.Logger;

public class LoggingBodyHttpServletRequest extends HttpServletRequestWrapper {

	private Logger logger;

	private volatile ServletInputStream servletInputStream;

	private ByteArrayOutputStream baos = new ByteArrayOutputStream(2048);

	public LoggingBodyHttpServletRequest(HttpServletRequest request, Logger logger) {
		super(request);
		this.logger = logger;
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		final ServletInputStream is = super.getInputStream();
		if (servletInputStream == null) {
			synchronized (this) {
				if (servletInputStream == null) {
					servletInputStream = new ServletInputStream() {

						@Override
						public int read() throws IOException {
							int i = is.read();
							if (i != -1)
								baos.write(i);
							return i;
						}

						@Override
						public void setReadListener(ReadListener readListener) {
							is.setReadListener(readListener);

						}

						@Override
						public boolean isReady() {
							return is.isReady();
						}

						@Override
						public boolean isFinished() {
							return is.isFinished();
						}

						@Override
						public void close() throws IOException {
							super.close();
							byte[] bytes = baos.toByteArray();
							baos.close();
							baos = null;
							String encoding = getCharacterEncoding();
							if (encoding == null)
								encoding = "UTF-8";
							logger.info("\n{}", new String(bytes, 0, bytes.length, encoding));
						}

					};
				}
			}
		}
		return servletInputStream;
	}

}