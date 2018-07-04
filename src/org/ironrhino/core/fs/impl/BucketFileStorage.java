package org.ironrhino.core.fs.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.ironrhino.core.fs.FileInfo;
import org.ironrhino.core.fs.Paged;

public abstract class BucketFileStorage extends AbstractFileStorage {

	public abstract String getBucket();

	@Override
	public boolean isBucketBased() {
		return true;
	}

	public boolean isUseHttps() {
		return false;
	}

	public String getDomain() {
		return null;
	}

	@Override
	public String getFileUrl(String path) {
		String domain = getDomain();
		if (domain == null || domain.isEmpty())
			return super.getFileUrl(path);
		StringBuilder sb = new StringBuilder(isUseHttps() ? "https" : "http");
		sb.append("://");
		sb.append(domain);
		if (!path.startsWith("/"))
			sb.append("/");
		sb.append(path);
		return sb.toString();
	}

	public int getBatchSize() {
		return DEFAULT_PAGE_SIZE;
	}

	@Override
	public List<String> listFiles(String path) throws IOException {
		List<String> list = new ArrayList<>();
		String marker = null;
		do {
			Paged<String> paged = listFiles(path, getBatchSize(), marker);
			list.addAll(paged.getResult());
			marker = paged.getNextMarker();
		} while (marker != null);
		Collections.sort(list);
		return list;
	}

	@Override
	public Paged<String> listFiles(String path, int limit, String marker) throws IOException {
		if (limit < 1 || limit > MAX_PAGE_SIZE)
			limit = DEFAULT_PAGE_SIZE;
		if (marker != null && marker.isEmpty())
			marker = null;
		return doListFiles(path, limit, marker);
	}

	protected Paged<String> defaultListFiles(String path, int limit, String marker) throws IOException {
		// Some implementation doesn't support pagination
		return super.listFiles(path, limit, marker);
	}

	protected abstract Paged<String> doListFiles(String path, int limit, String marker) throws IOException;

	@Override
	public List<FileInfo> listFilesAndDirectory(String path) throws IOException {
		List<FileInfo> list = new ArrayList<>();
		String marker = null;
		do {
			Paged<FileInfo> paged = listFilesAndDirectory(path, getBatchSize(), marker);
			list.addAll(paged.getResult());
			marker = paged.getNextMarker();
		} while (marker != null);
		list.sort(COMPARATOR);
		return list;
	}

	@Override
	public Paged<FileInfo> listFilesAndDirectory(String path, int limit, String marker) throws IOException {
		if (limit < 1 || limit > MAX_PAGE_SIZE)
			limit = DEFAULT_PAGE_SIZE;
		if (marker != null && marker.isEmpty())
			marker = null;
		return doListFilesAndDirectory(path, limit, marker);
	}

	protected Paged<FileInfo> defaultListFilesAndDirectory(String path, int limit, String marker) throws IOException {
		// Some implementation doesn't support pagination
		return super.listFilesAndDirectory(path, limit, marker);
	}

	protected abstract Paged<FileInfo> doListFilesAndDirectory(String path, int limit, String marker)
			throws IOException;

}
