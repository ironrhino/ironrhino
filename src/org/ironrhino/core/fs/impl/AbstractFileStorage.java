package org.ironrhino.core.fs.impl;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.fs.FileStorage;
import org.ironrhino.core.util.FileUtils;
import org.springframework.beans.factory.annotation.Value;

public abstract class AbstractFileStorage implements FileStorage {

	@Value("${fileStorage.baseUrl:}")
	protected String baseUrl;

	@Override
	public String getFileUrl(String path) {
		path = FileUtils.normalizePath(path);
		if (!path.startsWith("/"))
			path = '/' + path;
		return StringUtils.isNotBlank(baseUrl) ? baseUrl + path : path;
	}

}
