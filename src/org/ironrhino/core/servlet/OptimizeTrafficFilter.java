package org.ironrhino.core.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.digest.DigestUtils;

public class OptimizeTrafficFilter implements Filter {

	private boolean etag = true;

	private boolean compress = true;

	private int cacheSeconds = 86400;

	public void setEtag(boolean etag) {
		this.etag = etag;
	}

	public void setCompress(boolean compress) {
		this.compress = compress;
	}

	public void setCacheSeconds(int cacheSeconds) {
		this.cacheSeconds = cacheSeconds;
	}

	@Override
	public void doFilter(ServletRequest rq, ServletResponse rs,
			FilterChain chain) throws IOException, ServletException {
		if (!etag && !compress)
			chain.doFilter(rq, rs);
		HttpServletRequest request = (HttpServletRequest) rq;
		HttpServletResponse response = (HttpServletResponse) rs;
		LazyCommitResponseWrapper brw = new LazyCommitResponseWrapper(response);
		chain.doFilter(request, brw);
		byte[] bytes = brw.getContents();
		if (bytes != null) {
			boolean notmodified = false;
			if (etag) {
				String token = '"' + DigestUtils.md5Hex(bytes) + '"';
				response.setHeader("ETag", token);
				String previousToken = request.getHeader("If-None-Match");
				if (previousToken != null && previousToken.equals(token)) {
					response.setHeader("Last-Modified",
							request.getHeader("If-Modified-Since"));
					response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
					notmodified = true;
				} else {
					Calendar cal = Calendar.getInstance();
					cal.set(Calendar.MILLISECOND, 0);
					Date lastModified = cal.getTime();
					response.setDateHeader("Last-Modified",
							lastModified.getTime());
				}
			}
			if (!notmodified) {
				if (compress && brw.getContentType() != null
						&& brw.getContentType().indexOf("text") >= 0) {
					String acceptEncoding = request
							.getHeader("Accept-Encoding");
					if (acceptEncoding != null) {
						acceptEncoding = acceptEncoding.toLowerCase();
						if (acceptEncoding.indexOf("gzip") >= 0) {
							ByteArrayOutputStream boas = new ByteArrayOutputStream(
									10 * 1024);
							GZIPOutputStream gzos = new GZIPOutputStream(boas);
							gzos.write(bytes);
							gzos.flush();
							gzos.close();
							bytes = boas.toByteArray();
							response.setHeader(
									"Content-Encoding",
									acceptEncoding.indexOf("x-gzip") >= 0 ? "x-gzip"
											: "gzip");
						} else if (acceptEncoding.indexOf("deflate") >= 0) {
							// has problem with IE6
							ByteArrayOutputStream boas = new ByteArrayOutputStream(
									bytes.length / 2);
							DeflaterOutputStream dfos = new DeflaterOutputStream(
									boas, new Deflater(
											Deflater.BEST_COMPRESSION),
									bytes.length / 2);
							dfos.write(bytes);
							dfos.flush();
							dfos.close();
							bytes = boas.toByteArray();
							response.setHeader("Content-Encoding", "deflate");
						}
					}
				}

				response.setContentLength(bytes.length);
				response.addHeader("Cache-Control", "public");
				response.addHeader("Cache-Control", "max-age=" + cacheSeconds);
				Calendar cal = Calendar.getInstance();
				cal.set(Calendar.MILLISECOND, 0);
				cal.add(Calendar.SECOND, cacheSeconds);
				response.setDateHeader("Expires", cal.getTime().getTime());
				ServletOutputStream sos = response.getOutputStream();
				sos.write(bytes);
				sos.flush();
				sos.close();
			}
		}
	}

	@Override
	public void init(FilterConfig filterConfig) {
		if ("false".equals(filterConfig.getInitParameter("etag")))
			etag = false;
		if ("false".equals(filterConfig.getInitParameter("compress")))
			compress = false;
		if (filterConfig.getInitParameter("cacheSeconds") != null)
			cacheSeconds = Integer.valueOf(filterConfig
					.getInitParameter("cacheSeconds"));
	}

	@Override
	public void destroy() {

	}

}
