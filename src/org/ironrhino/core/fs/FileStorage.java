package org.ironrhino.core.fs;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface FileStorage {

	public void write(InputStream is, String path) throws IOException;

	public InputStream open(String path) throws IOException;

	public boolean mkdir(String path);

	public boolean delete(String path);

	public boolean exists(String path);

	public boolean rename(String fromPath, String toPath);

	public boolean isDirectory(String path);

	public long getLastModified(String path);

	public List<String> listFiles(String path);

	public Map<String, Boolean> listFilesAndDirectory(String path);
	
	public String getFileUrl(String path);

}
