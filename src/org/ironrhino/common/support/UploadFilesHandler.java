package org.ironrhino.common.support;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.ironrhino.common.action.UploadAction;
import org.ironrhino.core.fs.FileStorage;
import org.ironrhino.core.servlet.AccessHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Integer.MIN_VALUE)
public class UploadFilesHandler implements AccessHandler {

	@Autowired
	private FileStorage fileStorage;

	@Autowired
	private ServletContext servletContext;

	private String pattern = "/assets" + UploadAction.UPLOAD_DIR + "/*";

	@Override
	public String getPattern() {
		return pattern;
	}

	@Override
	public boolean handle(HttpServletRequest request,
			HttpServletResponse response) {
		long since = request.getDateHeader("If-Modified-Since");
		String uri = request.getRequestURI();
		String path = uri.substring(uri.indexOf('/', 1));
		try {
			path = URLDecoder.decode(path, "UTF-8");
			long lastModified = fileStorage.getLastModified(path);
			lastModified = lastModified / 1000 * 1000;
			if (since > 0 && since == lastModified) {
				response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
				return true;
			}
			InputStream is = fileStorage.open(path);
			if (is == null) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return true;
			}
			if (lastModified > 0)
				response.setDateHeader("Last-Modified", lastModified);
			String filename = path.substring(path.lastIndexOf("/") + 1);
			String contentType = servletContext.getMimeType(filename);
			if (contentType != null)
				response.setContentType(contentType);
			OutputStream os = response.getOutputStream();
			try {
				IOUtils.copy(is, os);
				os.close();
			} catch (Exception e) {
				// supress ClientAbortException
			}
			is.close();
		} catch (FileNotFoundException fne) {
			try {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return true;
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
			try {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				return true;
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		return true;
	}

}
