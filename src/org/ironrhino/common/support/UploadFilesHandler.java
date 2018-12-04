package org.ironrhino.common.support;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ironrhino.core.fs.FileStorage;
import org.ironrhino.core.servlet.AccessHandler;
import org.ironrhino.core.spring.configuration.PriorityQualifier;
import org.ironrhino.core.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import lombok.Getter;
import lombok.Setter;

@Component
@Order(2)
public class UploadFilesHandler extends AccessHandler {

	public static final String DEFAULT_PATH_PREFIX = "/assets";

	public static final String DEFAULT_UPLOAD_DIR = "/upload";

	@Autowired
	@Qualifier("fileStorage")
	@PriorityQualifier
	private FileStorage uploadFileStorage;

	@Autowired
	private ServletContext servletContext;

	@Getter
	@Setter
	@Value("${uploadFilesHandler.pathPrefix:" + DEFAULT_PATH_PREFIX + "}")
	protected String pathPrefix = DEFAULT_PATH_PREFIX;

	@Getter
	@Setter
	@Value("${uploadFilesHandler.uploadDir:" + DEFAULT_UPLOAD_DIR + "}")
	protected String uploadDir = DEFAULT_UPLOAD_DIR;

	private String pattern;

	private boolean prependDefaultUploadDirForBucketBased;

	@PostConstruct
	private void init() {
		pathPrefix = normalize(pathPrefix);
		uploadDir = normalize(uploadDir);
		if (uploadFileStorage.isBucketBased()) {
			uploadDir = "";
			prependDefaultUploadDirForBucketBased = DEFAULT_PATH_PREFIX.equals(pathPrefix);
			if (prependDefaultUploadDirForBucketBased)
				pattern = pathPrefix + DEFAULT_UPLOAD_DIR + "/*";
			else
				pattern = pathPrefix + "/*";
		} else {
			pattern = pathPrefix + uploadDir + "/*";
		}
	}

	@Override
	public String getPattern() {
		return pattern;
	}

	public String getFileUrl(String path) {
		StringBuilder sb = new StringBuilder(pathPrefix);
		if (prependDefaultUploadDirForBucketBased)
			sb.append(UploadFilesHandler.DEFAULT_UPLOAD_DIR);
		sb.append(path);
		return sb.toString();
	}

	@Override
	public boolean handle(HttpServletRequest request, HttpServletResponse response) {
		long since = request.getDateHeader("If-Modified-Since");
		String uri = RequestUtils.getRequestUri(request);
		String path = uri.substring(pathPrefix.length());
		if (prependDefaultUploadDirForBucketBased)
			path = path.substring(DEFAULT_UPLOAD_DIR.length());
		try {
			path = URLDecoder.decode(path, "UTF-8");
			long lastModified = uploadFileStorage.getLastModified(path);
			lastModified = lastModified / 1000 * 1000;
			if (since > 0 && since == lastModified) {
				response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
				return true;
			}
			try (InputStream is = uploadFileStorage.open(path)) {
				if (is == null) {
					response.setStatus(HttpServletResponse.SC_NOT_FOUND);
					return true;
				}
				if (lastModified > 0)
					response.setDateHeader("Last-Modified", lastModified);
				String filename = path.substring(path.lastIndexOf("/") + 1);
				String contentType = servletContext.getMimeType(filename);
				if (contentType != null)
					response.setContentType(contentType);
				try (OutputStream os = response.getOutputStream()) {
					StreamUtils.copy(is, os);
				} catch (Exception e) {
					// supress ClientAbortException
				}
				return true;
			}
		} catch (FileNotFoundException fne) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return true;
		}
	}

	private static String normalize(String path) {
		if (path != null && path.length() > 0) {
			if (!path.startsWith("/"))
				path = "/" + path;
			if (path.endsWith("/"))
				path = StringUtils.trimTrailingCharacter(path, '/');
			if (path.equals("/"))
				path = "";
		}
		return path;
	}

}
