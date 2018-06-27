package org.ironrhino.core.fs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.ironrhino.core.util.ValueThenKeyComparator;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;

public interface FileStorage {

	ValueThenKeyComparator<String, Boolean> COMPARATOR = new ValueThenKeyComparator<String, Boolean>() {
		@Override
		protected int compareValue(Boolean a, Boolean b) {
			return b.compareTo(a);
		}
	};

	public default boolean isBucketBased() {
		try {
			return getClass().getMethod("getBucket") != null;
		} catch (NoSuchMethodException | SecurityException e) {
			return false;
		}
	}

	public default boolean isRelativeProtocolAllowed() {
		return isBucketBased();
	}

	public default void migrateTo(FileStorage target, String directory, boolean removeSourceFiles) throws IOException {
		if (directory == null)
			directory = "/";
		if (!directory.endsWith("/"))
			directory = directory + "/";
		Map<String, Boolean> files = this.listFilesAndDirectory(directory);
		for (Map.Entry<String, Boolean> entry : files.entrySet()) {
			String path = directory + entry.getKey();
			if (entry.getValue()) {
				target.write(this.open(path), path);
				if (removeSourceFiles)
					this.delete(path);
			} else {
				migrateTo(target, path, removeSourceFiles);
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
		String contentType = null;
		int index = path.lastIndexOf('/');
		Optional<MediaType> type = MediaTypeFactory.getMediaType(index >= 0 ? path.substring(index + 1) : path);
		if (type.isPresent())
			contentType = type.get().toString();
		write(is, path, contentLength, contentType);
	}

	public default void write(InputStream is, String path, long contentLength, String contentType) throws IOException {
		write(is, path);
	}

	public void write(InputStream is, String path) throws IOException;

	public InputStream open(String path) throws IOException;

	public boolean mkdir(String path) throws IOException;

	public boolean delete(String path) throws IOException;

	public boolean exists(String path) throws IOException;

	public boolean rename(String fromPath, String toPath) throws IOException;

	public boolean isDirectory(String path) throws IOException;

	public long getLastModified(String path) throws IOException;

	public List<String> listFiles(String path) throws IOException;

	public Map<String, Boolean> listFilesAndDirectory(String path) throws IOException;

	public String getFileUrl(String path);

}
