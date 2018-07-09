package org.ironrhino.core.fs.impl;

import static org.ironrhino.core.metadata.Profiles.CLOUD;
import static org.ironrhino.core.metadata.Profiles.CLUSTER;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.ironrhino.core.fs.FileInfo;
import org.ironrhino.core.spring.configuration.ServiceImplementationConditional;
import org.ironrhino.core.util.ErrorMessage;
import org.ironrhino.core.util.FileUtils;
import org.ironrhino.core.util.LimitExceededException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import lombok.Data;

@Component("fileStorage")
@ServiceImplementationConditional(profiles = { CLOUD, CLUSTER })
public class MongoFileStorage extends AbstractFileStorage {

	@Autowired
	private MongoTemplate mongoTemplate;

	@PostConstruct
	public void afterPropertiesSet() {
		if (!mongoTemplate.collectionExists(File.class))
			mongoTemplate.createCollection(File.class);
	}

	@Override
	public void write(InputStream is, String path) throws IOException {
		if (path.equals("") || path.endsWith("/"))
			throw new ErrorMessage("path " + path + " is directory");
		path = normalizePath(path);
		int lastIndex = path.lastIndexOf('/');
		if (lastIndex > 0)
			mkdir(path.substring(0, lastIndex));
		try (InputStream ins = is; ByteArrayOutputStream os = new ByteArrayOutputStream(512 * 1024)) {
			IOUtils.copy(is, os);
			File file = new File();
			file.setPath(path);
			file.setData(os.toByteArray());
			file.setLastModified(System.currentTimeMillis());
			mongoTemplate.save(file);
		}
	}

	@Override
	public InputStream open(String path) throws IOException {
		path = normalizePath(path);
		File file = mongoTemplate.findById(path, File.class);
		if (file == null || file.isDirectory())
			return null;
		return new ByteArrayInputStream(file.getData());
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

	protected boolean doMkdir(String path) {
		if (path.equals("") || path.equals("/"))
			return true;
		if (!path.endsWith("/"))
			path += "/";
		path = normalizePath(path);
		File file = mongoTemplate.findById(path, File.class);
		if (file != null)
			return file.isDirectory();
		file = new File();
		file.setPath(path);
		file.setDirectory(true);
		file.setLastModified(System.currentTimeMillis());
		mongoTemplate.save(file);
		return true;
	}

	@Override
	public boolean delete(String path) {
		if (path.equals("") || path.equals("/"))
			return false;
		path = normalizePath(path);
		if (path.lastIndexOf('.') <= path.lastIndexOf('/') && isDirectory(path)) {
			if (!path.endsWith("/"))
				path += "/";
			if (listFilesAndDirectory(path).size() > 0)
				return false;
			mongoTemplate.remove(new Query(where("path").is(path)), File.class);
			return true;
		} else {
			mongoTemplate.remove(new Query(where("path").is(path)), File.class);
			return true;
		}
	}

	@Override
	public long getLastModified(String path) {
		if (path.equals("") || path.equals("/"))
			return 0;
		path = normalizePath(path);
		File file = mongoTemplate.findById(path, File.class);
		return file != null ? file.getLastModified() : 0;
	}

	@Override
	public boolean exists(String path) {
		if (path.equals("") || path.equals("/"))
			return true;
		if (path.endsWith("/"))
			return isDirectory(path);
		path = normalizePath(path);
		return mongoTemplate.findById(path, File.class) != null;
	}

	@Override
	public boolean rename(String fromPath, String toPath) {
		fromPath = normalizePath(fromPath);
		toPath = normalizePath(toPath);
		if (fromPath.equals("/") || toPath.equals("/"))
			return false;
		String s1 = fromPath.substring(0, fromPath.lastIndexOf('/'));
		String s2 = toPath.substring(0, fromPath.lastIndexOf('/'));
		if (!s1.equals(s2))
			return false;
		File fromfile = mongoTemplate.findById(fromPath, File.class);
		if (fromfile == null)
			return false;
		File tofile = mongoTemplate.findById(toPath, File.class);
		if (tofile == null) {
			tofile = new File();
			tofile.setPath(toPath);
		}
		tofile.setData(fromfile.getData());
		tofile.setLastModified(fromfile.getLastModified());
		mongoTemplate.save(tofile);
		mongoTemplate.remove(fromfile);
		return true;
	}

	@Override
	public boolean isDirectory(String path) {
		if (path.equals("") || path.equals("/"))
			return true;
		if (!path.endsWith("/"))
			path += '/';
		path = normalizePath(path);
		File file = mongoTemplate.findById(path, File.class);
		return file != null && file.isDirectory();
	}

	@Override
	public List<FileInfo> listFiles(String path) {
		if (!path.endsWith("/"))
			path += "/";
		path = normalizePath(path);
		List<FileInfo> list = new ArrayList<>();
		String regex;
		if ("".equals(path)) {
			regex = "^[^/]+$";
		} else {
			regex = "^" + path.replaceAll("\\.", "\\\\.") + "[^/]+$";
		}
		List<File> files = mongoTemplate.find(new Query(where("path").regex(regex)), File.class);
		for (File f : files) {
			String name = f.getPath();
			name = name.substring(path.length());
			list.add(new FileInfo(name, true, f.getData().length, f.getLastModified()));
			if (list.size() > MAX_PAGE_SIZE)
				throw new LimitExceededException("Exceed max size:" + MAX_PAGE_SIZE);
		}
		list.sort(COMPARATOR);
		return list;
	}

	@Override
	public List<FileInfo> listFilesAndDirectory(String path) {
		if (!path.endsWith("/"))
			path += "/";
		path = normalizePath(path);
		List<FileInfo> list = new ArrayList<>();
		String regex;
		if ("".equals(path)) {
			regex = "^[^/]+\\/?$";
		} else {
			regex = "^" + path.replaceAll("\\.", "\\\\.") + "[^/]+\\/?$";
		}
		List<File> files = mongoTemplate.find(new Query(where("path").regex(regex)), File.class);
		for (File f : files) {
			String name = f.getPath();
			name = name.substring(path.length());
			if (f.isDirectory() && name.endsWith("/"))
				name = name.substring(0, name.length() - 1);
			list.add(new FileInfo(name, !f.isDirectory(), f.isDirectory() ? 0 : f.getData().length,
					f.getLastModified()));
			if (list.size() > MAX_PAGE_SIZE)
				throw new LimitExceededException("Exceed max size:" + MAX_PAGE_SIZE);
		}
		list.sort(COMPARATOR);
		return list;
	}

	protected String normalizePath(String path) {
		return FileUtils.normalizePath(StringUtils.trimLeadingCharacter(path, '/'));
	}

	@Data
	private static class File implements Serializable {

		private static final long serialVersionUID = -7690537474523537861L;

		@Id
		private String path;

		private boolean directory;

		private long lastModified;

		private byte[] data;

	}

}
