package org.ironrhino.core.fs.impl;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.fs.FileStorage;
import org.ironrhino.core.util.FileUtils;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.annotation.Value;

import lombok.Getter;

public abstract class AbstractFileStorage implements FileStorage, BeanNameAware {

	@Value("${fileStorage.baseUrl:}")
	protected String baseUrl;

	@Getter
	private String name;

	@Override
	public String getFileUrl(String path) {
		path = FileUtils.normalizePath(path);
		if (!path.startsWith("/"))
			path = '/' + path;
		return StringUtils.isNotBlank(baseUrl) ? baseUrl + path : path;
	}

	@Override
	public void setBeanName(String beanName) {
		if (beanName.equalsIgnoreCase("FileStorage")) {
			name = FileStorage.super.getName();
		} else {
			if (beanName.endsWith("FileStorage"))
				beanName = beanName.substring(0, beanName.length() - "FileStorage".length());
			name = beanName;
		}
	}

}
