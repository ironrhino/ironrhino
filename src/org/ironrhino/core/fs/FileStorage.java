package org.ironrhino.core.fs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;

import org.springframework.http.MediaTypeFactory;

public interface FileStorage {

	int DEFAULT_PAGE_SIZE = 100;

	int MAX_PAGE_SIZE = 10000;

	Comparator<FileInfo> COMPARATOR = Comparator.comparing(FileInfo::isFile).thenComparing(FileInfo::getName);

	public default String getName() {
		return "default";
	}

	public default boolean isBucketBased() {
		return false;
	}

	public default boolean isRelativeProtocolAllowed() {
		return isBucketBased();
	}

	public default void migrateTo(FileStorage target, String directory, boolean removeSourceFiles) throws IOException {
		if (directory == null)
			directory = "/";
		if (!directory.endsWith("/"))
			directory = directory + "/";
		boolean paging = this.isBucketBased();
		if (paging) {
			String marker = null;
			Paged<FileInfo> files = null;
			do {
				files = this.listFilesAndDirectory(directory, 100, marker);
				for (FileInfo entry : files.getResult()) {
					String path = directory + entry.getName();
					if (entry.isFile()) {
						target.write(this.open(path), path);
						if (removeSourceFiles)
							this.delete(path);
					} else {
						migrateTo(target, path, removeSourceFiles);
					}
				}
				marker = files.getNextMarker();
			} while (marker != null);
		} else {
			List<FileInfo> files = this.listFilesAndDirectory(directory);
			for (FileInfo entry : files) {
				String path = directory + entry.getName();
				if (entry.isFile()) {
					target.write(this.open(path), path);
					if (removeSourceFiles)
						this.delete(path);
				} else {
					migrateTo(target, path, removeSourceFiles);
				}
			}
		}
		if (removeSourceFiles && !directory.equals("/"))
			this.delete(directory);
	}

	public default void write(File file, String path) throws IOException {
		try (FileInputStream is = new FileInputStream(file)) {
			write(is, path, file.length());
		}
	}

	public default void write(InputStream is, String path, long contentLength) throws IOException {
		int index = path.lastIndexOf('/');
		String contentType = MediaTypeFactory.getMediaType(index >= 0 ? path.substring(index + 1) : path)
				.map(Object::toString).orElse(null);
		write(is, path, contentLength, contentType);
	}

	public default void write(InputStream is, String path, long contentLength, String contentType) throws IOException {
		write(is, path);
	}

	public void write(InputStream is, String path) throws IOException;

	public InputStream open(String path) throws IOException;

	public boolean mkdir(String path);

	public boolean delete(String path);

	public boolean exists(String path);

	public boolean rename(String fromPath, String toPath);

	public boolean isDirectory(String path);

	public long getLastModified(String path);

	public List<FileInfo> listFiles(String path);

	public default Paged<FileInfo> listFiles(String path, int limit, String marker) {
		if (limit < 1 || limit > MAX_PAGE_SIZE)
			limit = DEFAULT_PAGE_SIZE;
		if (marker != null && marker.isEmpty())
			marker = null;
		return Paged.from(listFiles(path), limit, marker, FileInfo::getName);
	}

	public List<FileInfo> listFilesAndDirectory(String path);

	public default Paged<FileInfo> listFilesAndDirectory(String path, int limit, String marker) {
		if (limit < 1 || limit > MAX_PAGE_SIZE)
			limit = DEFAULT_PAGE_SIZE;
		if (marker != null && marker.isEmpty())
			marker = null;
		return Paged.from(listFilesAndDirectory(path), limit, marker, FileInfo::getName);
	}

	public String getFileUrl(String path);

}
