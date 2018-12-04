package org.ironrhino.core.fs.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.ironrhino.core.fs.FileInfo;
import org.ironrhino.core.fs.Paged;
import org.ironrhino.core.util.ErrorMessage;
import org.ironrhino.core.util.FileUtils;
import org.ironrhino.core.util.LimitExceededException;
import org.springframework.util.StringUtils;

public abstract class BucketFileStorage extends AbstractFileStorage {

	public abstract String getBucket();

	@Override
	public boolean isBucketBased() {
		return true;
	}

	@Override
	public boolean isRelativeProtocolAllowed() {
		return !StringUtils.isEmpty(getDomain());
	}

	public boolean isUseHttps() {
		return false;
	}

	public String getDomain() {
		return null;
	}

	protected String normalizePath(String path) {
		return FileUtils.normalizePath(StringUtils.trimLeadingCharacter(path, '/'));
	}

	@Override
	public boolean mkdir(String path) {
		if (path.equals("") || path.equals("/"))
			return true;
		path = normalizePath(path);
		int lastIndex = path.lastIndexOf('/');
		if (lastIndex > 0) {
			int index = 0;
			while (index < lastIndex) {
				index = path.indexOf('/', index + 1);
				if (index < 0)
					break;
				if (!doMkdir(path.substring(0, index)))
					return false;
			}
		}
		return doMkdir(path);
	}

	protected abstract boolean doMkdir(String path);

	@Override
	public void write(InputStream is, String path, long contentLength, String contentType) throws IOException {
		if (path.equals("") || path.endsWith("/"))
			throw new ErrorMessage("path " + path + " is directory");
		path = normalizePath(path);
		int lastIndex = path.lastIndexOf('/');
		if (lastIndex > 0)
			mkdir(path.substring(0, lastIndex));
		if (contentLength < 0) {
			if (is instanceof ByteArrayInputStream)
				contentLength = ((ByteArrayInputStream) is).available();
		}
		doWrite(is, path, contentLength, contentType);
	}

	@Override
	public void write(InputStream is, String path) throws IOException {
		write(is, path, -1, null);
	}

	protected abstract void doWrite(InputStream is, String path, long contentLength, String contentType)
			throws IOException;

	@Override
	public String getFileUrl(String path) {
		String domain = getDomain();
		if (StringUtils.isEmpty(domain))
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
	public List<FileInfo> listFiles(String path) {
		List<FileInfo> list = new ArrayList<>();
		String marker = null;
		do {
			Paged<FileInfo> paged = listFiles(path, getBatchSize(), marker);
			list.addAll(paged.getResult());
			if (list.size() > MAX_PAGE_SIZE)
				throw new LimitExceededException("Exceed max size:" + MAX_PAGE_SIZE);
			marker = paged.getNextMarker();
		} while (marker != null);
		list.sort(COMPARATOR);
		return list;
	}

	@Override
	public Paged<FileInfo> listFiles(String path, int limit, String marker) {
		if (limit < 1 || limit > MAX_PAGE_SIZE)
			limit = DEFAULT_PAGE_SIZE;
		if (marker != null && marker.isEmpty())
			marker = null;
		List<FileInfo> list = new ArrayList<>();
		String nextMarker = marker;
		do {
			// result.size() < limit if mixed with directory
			Paged<FileInfo> result = doListFiles(path, limit - list.size(), nextMarker);
			list.addAll(result.getResult());
			nextMarker = result.getNextMarker();
		} while (list.size() < limit && nextMarker != null);
		return new Paged<>(marker, nextMarker, list);
	}

	protected Paged<FileInfo> defaultListFiles(String path, int limit, String marker) {
		// Some implementation doesn't support pagination
		return super.listFiles(path, limit, marker);
	}

	protected Paged<FileInfo> doListFiles(String path, int limit, String marker) {
		Paged<FileInfo> all = doListFilesAndDirectory(path, limit, marker);
		return new Paged<>(all.getMarker(), all.getNextMarker(),
				all.getResult().stream().filter(FileInfo::isFile).collect(Collectors.toList()));
	}

	@Override
	public List<FileInfo> listFilesAndDirectory(String path) {
		List<FileInfo> list = new ArrayList<>();
		String marker = null;
		do {
			Paged<FileInfo> paged = listFilesAndDirectory(path, getBatchSize(), marker);
			list.addAll(paged.getResult());
			if (list.size() > MAX_PAGE_SIZE)
				throw new LimitExceededException("Exceed max size:" + MAX_PAGE_SIZE);
			marker = paged.getNextMarker();
		} while (marker != null);
		list.sort(COMPARATOR);
		return list;
	}

	@Override
	public Paged<FileInfo> listFilesAndDirectory(String path, int limit, String marker) {
		if (limit < 1 || limit > MAX_PAGE_SIZE)
			limit = DEFAULT_PAGE_SIZE;
		if (marker != null && marker.isEmpty())
			marker = null;
		return doListFilesAndDirectory(path, limit, marker);
	}

	protected Paged<FileInfo> defaultListFilesAndDirectory(String path, int limit, String marker) {
		// Some implementation doesn't support pagination
		return super.listFilesAndDirectory(path, limit, marker);
	}

	protected abstract Paged<FileInfo> doListFilesAndDirectory(String path, int limit, String marker);

}
