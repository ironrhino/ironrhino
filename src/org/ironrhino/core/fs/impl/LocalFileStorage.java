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
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.ironrhino.core.fs.FileInfo;
import org.ironrhino.core.spring.configuration.ServiceImplementationConditional;
import org.ironrhino.core.util.FileUtils;
import org.ironrhino.core.util.LimitExceededException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Component("fileStorage")
@ServiceImplementationConditional(profiles = { DEFAULT, DUAL })
@Slf4j
public class LocalFileStorage extends AbstractFileStorage {

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
				log.error("mkdirs error:" + directory.getAbsolutePath());
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
	public boolean rename(String fromPath, String toPath) {
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
		if (path.equals("") || path.equals("/"))
			return true;
		path = normalizePath(path);
		return new File(directory, path).isDirectory();
	}

	@Override
	public List<FileInfo> listFiles(String path) {
		path = normalizePath(path);
		final List<FileInfo> list = new ArrayList<>();
		new File(directory, path).listFiles(f -> {
			if (f.isFile()) {
				list.add(new FileInfo(f.getName(), true, f.length(), f.lastModified()));
				if (list.size() > MAX_PAGE_SIZE)
					throw new LimitExceededException("Exceed max size:" + MAX_PAGE_SIZE);
			}
			return false;
		});
		list.sort(COMPARATOR);
		return list;
	}

	@Override
	public List<FileInfo> listFilesAndDirectory(String path) {
		path = normalizePath(path);
		final List<FileInfo> list = new ArrayList<>();
		new File(directory, path).listFiles(f -> {
			list.add(new FileInfo(f.getName(), f.isFile(), f.length(), f.lastModified()));
			if (list.size() > MAX_PAGE_SIZE)
				throw new LimitExceededException("Exceed max size:" + MAX_PAGE_SIZE);
			return false;
		});
		list.sort(COMPARATOR);
		return list;
	}

	private String normalizePath(String path) {
		if (!path.startsWith("/"))
			path = "/" + path;
		return FileUtils.normalizePath(path);
	}

}
