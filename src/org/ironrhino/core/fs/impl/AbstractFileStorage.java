package org.ironrhino.core.fs.impl;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.fs.FileStorage;
import org.ironrhino.core.util.ValueThenKeyComparator;
import org.springframework.beans.factory.annotation.Value;

import com.google.common.io.Files;

public abstract class AbstractFileStorage implements FileStorage {

	@Value("${fileStorage.baseUrl:}")
	protected String baseUrl;

	@Override
	public String getFileUrl(String path) {
		path = Files.simplifyPath(path);
		if (!path.startsWith("/"))
			path = '/' + path;
		return StringUtils.isNotBlank(baseUrl) ? baseUrl + path : path;
	}

	protected ValueThenKeyComparator<String, Boolean> comparator = new ValueThenKeyComparator<String, Boolean>() {
		@Override
		protected int compareValue(Boolean a, Boolean b) {
			return b.compareTo(a);
		}
	};
}
