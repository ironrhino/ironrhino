package org.ironrhino.core.fs.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.ironrhino.core.fs.FileInfo;
import org.ironrhino.core.fs.FileStorage;
import org.ironrhino.core.fs.Paged;

import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
public class CompositeFileStorage implements FileStorage {

	private final FileStorage mainFileStorage;

	private final FileStorage fallbackFileStorage;

	@Setter
	private boolean merging;

	@Override
	public void write(InputStream is, String path) throws IOException {
		mainFileStorage.write(is, path);
	}

	@Override
	public boolean mkdir(String path) {
		return mainFileStorage.mkdir(path);
	}

	@Override
	public boolean delete(String path) {
		return mainFileStorage.delete(path);
	}

	@Override
	public boolean rename(String fromPath, String toPath) {
		return mainFileStorage.rename(fromPath, toPath);
	}

	@Override
	public InputStream open(String path) throws IOException {
		InputStream is = mainFileStorage.open(path);
		return is != null ? is : fallbackFileStorage.open(path);
	}

	@Override
	public boolean exists(String path) {
		boolean b = mainFileStorage.exists(path);
		return b ? b : fallbackFileStorage.exists(path);
	}

	@Override
	public boolean isDirectory(String path) {
		boolean b = mainFileStorage.isDirectory(path);
		return b ? b : fallbackFileStorage.isDirectory(path);
	}

	@Override
	public long getLastModified(String path) {
		long m = mainFileStorage.getLastModified(path);
		return m > 0 ? m : fallbackFileStorage.getLastModified(path);
	}

	@Override
	public List<FileInfo> listFiles(String path) {
		if (merging) {
			List<FileInfo> list1 = mainFileStorage.listFiles(path);
			List<FileInfo> list2 = fallbackFileStorage.listFiles(path);
			List<FileInfo> result = new ArrayList<>(list1.size() + list2.size());
			result.addAll(list1);
			result.addAll(list2);
			result.sort(COMPARATOR);
			return result;
		} else {
			FileStorage fs = mainFileStorage.isDirectory(path) ? mainFileStorage : fallbackFileStorage;
			return fs.listFiles(path);
		}
	}

	@Override
	public Paged<FileInfo> listFiles(String path, int limit, String marker) {
		if (merging)
			return FileStorage.super.listFiles(path, limit, marker);
		FileStorage fs = mainFileStorage.isDirectory(path) ? mainFileStorage : fallbackFileStorage;
		return fs.listFiles(path, limit, marker);
	}

	@Override
	public List<FileInfo> listFilesAndDirectory(String path) {
		if (merging) {
			List<FileInfo> list1 = mainFileStorage.listFilesAndDirectory(path);
			List<FileInfo> list2 = fallbackFileStorage.listFilesAndDirectory(path);
			List<FileInfo> result = new ArrayList<>(list1.size() + list2.size());
			result.addAll(list1);
			result.addAll(list2);
			result.sort(COMPARATOR);
			return result;
		} else {
			FileStorage fs = mainFileStorage.isDirectory(path) ? mainFileStorage : fallbackFileStorage;
			return fs.listFilesAndDirectory(path);
		}
	}

	@Override
	public Paged<FileInfo> listFilesAndDirectory(String path, int limit, String marker) {
		if (merging)
			return FileStorage.super.listFilesAndDirectory(path, limit, marker);
		FileStorage fs = mainFileStorage.isDirectory(path) ? mainFileStorage : fallbackFileStorage;
		return fs.listFilesAndDirectory(path, limit, marker);
	}

	@Override
	public String getFileUrl(String path) {
		FileStorage fs = mainFileStorage.exists(path) ? mainFileStorage : fallbackFileStorage;
		return fs.getFileUrl(path);
	}

}
