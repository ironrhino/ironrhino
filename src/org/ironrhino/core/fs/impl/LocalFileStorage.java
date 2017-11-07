package org.ironrhino.core.fs.impl;

import static org.ironrhino.core.metadata.Profiles.DEFAULT;
import static org.ironrhino.core.metadata.Profiles.DUAL;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.ironrhino.core.spring.configuration.ServiceImplementationConditional;
import org.ironrhino.core.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import lombok.Getter;
import lombok.Setter;

@Component("fileStorage")
@ServiceImplementationConditional(profiles = { DEFAULT, DUAL })
public class LocalFileStorage extends AbstractFileStorage {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Getter
	@Setter
	@Value("${fileStorage.uri:file:///${app.context}/assets/}")
	protected URI uri;

	private File directory;

	@PostConstruct
	public void afterPropertiesSet() {
		Assert.notNull(uri, "uri shouldn't be null");
		this.directory = new File(uri);
		if (this.directory.isFile())
			throw new IllegalStateException(directory + " is not directory");
		if (!this.directory.exists())
			if (!this.directory.mkdirs())
				logger.error("mkdirs error:" + directory.getAbsolutePath());
	}

	@Override
	public void write(InputStream is, String path) throws IOException {
		path = normalizePath(path);
		File dest = new File(directory, path);
		dest.getParentFile().mkdirs();
		try (InputStream ins = is; FileOutputStream os = new FileOutputStream(dest)) {
			IOUtils.copy(ins, os);
		}
	}

	@Override
	public InputStream open(String path) throws IOException {
		path = normalizePath(path);
		File file = new File(directory, path);
		if (!file.exists() || file.isDirectory())
			return null;
		return new FileInputStream(file);
	}

	@Override
	public boolean mkdir(String path) {
		path = normalizePath(path);
		return new File(directory, path).mkdirs();
	}

	@Override
	public boolean delete(String path) {
		path = normalizePath(path);
		return new File(directory, path).delete();
	}

	@Override
	public long getLastModified(String path) {
		path = normalizePath(path);
		return new File(directory, path).lastModified();
	}

	@Override
	public boolean exists(String path) {
		path = normalizePath(path);
		return new File(directory, path).exists();
	}

	@Override
	public boolean rename(String fromPath, String toPath) throws IOException {
		fromPath = normalizePath(directory.getPath() + "/" + fromPath);
		toPath = normalizePath(directory.getPath() + "/" + toPath);
		File source = new File(fromPath);
		File target = new File(toPath);
		if (source.getParent().equals(target.getParent())) {
			return source.renameTo(target);
		} else {
			return false;
		}
	}

	@Override
	public boolean isDirectory(String path) {
		path = normalizePath(path);
		return new File(directory, path).isDirectory();
	}

	@Override
	public List<String> listFiles(String path) {
		path = normalizePath(path);
		final List<String> list = new ArrayList<>();
		new File(directory, path).listFiles(f -> {
			if (f.isFile()) {
				list.add(f.getName());
			}
			return false;
		});
		return list;
	}

	@Override
	public Map<String, Boolean> listFilesAndDirectory(String path) {
		path = normalizePath(path);
		final Map<String, Boolean> map = new HashMap<>();
		new File(directory, path).listFiles(f -> {
			map.put(f.getName(), f.isFile());
			return false;
		});
		List<Map.Entry<String, Boolean>> list = new ArrayList<>(map.entrySet());
		Collections.sort(list, COMPARATOR);
		Map<String, Boolean> sortedMap = new LinkedHashMap<>();
		for (Map.Entry<String, Boolean> entry : list)
			sortedMap.put(entry.getKey(), entry.getValue());
		return sortedMap;
	}

	private String normalizePath(String path) {
		if (!path.startsWith("/"))
			path = "/" + path;
		return FileUtils.normalizePath(path);
	}

}
