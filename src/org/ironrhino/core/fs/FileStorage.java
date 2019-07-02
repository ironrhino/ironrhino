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

	default String getName() {
		return "default";
	}

	default boolean isBucketBased() {
		return false;
	}

	default boolean isRelativeProtocolAllowed() {
		return false;
	}

	default void migrateTo(FileStorage target, String directory, boolean removeSourceFiles) throws IOException {
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

	default void write(File file, String path) throws IOException {
		try (FileInputStream is = new FileInputStream(file)) {
			write(is, path, file.length());
		}
	}

	default void write(InputStream is, String path, long contentLength) throws IOException {
		int index = path.lastIndexOf('/');
		String contentType = MediaTypeFactory.getMediaType(index >= 0 ? path.substring(index + 1) : path)
				.map(Object::toString).orElse(null);
		write(is, path, contentLength, contentType);
	}

	default void write(InputStream is, String path, long contentLength, String contentType) throws IOException {
		write(is, path);
	}

	void write(InputStream is, String path) throws IOException;

	InputStream open(String path) throws IOException;

	boolean mkdir(String path);

	boolean delete(String path);

	boolean exists(String path);

	boolean rename(String fromPath, String toPath);

	boolean isDirectory(String path);

	long getLastModified(String path);

	List<FileInfo> listFiles(String path);

	default Paged<FileInfo> listFiles(String path, int limit, String marker) {
		if (limit < 1 || limit > MAX_PAGE_SIZE)
			limit = DEFAULT_PAGE_SIZE;
		if (marker != null && marker.isEmpty())
			marker = null;
		return Paged.from(listFiles(path), limit, marker, FileInfo::getName);
	}

	List<FileInfo> listFilesAndDirectory(String path);

	default Paged<FileInfo> listFilesAndDirectory(String path, int limit, String marker) {
		if (limit < 1 || limit > MAX_PAGE_SIZE)
			limit = DEFAULT_PAGE_SIZE;
		if (marker != null && marker.isEmpty())
			marker = null;
		return Paged.from(listFilesAndDirectory(path), limit, marker, FileInfo::getName);
	}

	String getFileUrl(String path);

}
