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
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.ironrhino.core.fs.FileInfo;
import org.ironrhino.core.spring.configuration.ServiceImplementationConditional;
import org.ironrhino.core.util.FileUtils;
import org.ironrhino.core.util.LimitExceededException;
import org.ironrhino.core.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

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
		path = normalizePath(path);
		if (path.equals("/"))
			throw new IOException("cannot direct access path /");
		File file = mongoTemplate.findById(path, File.class);
		if (file == null) {
			int lastIndex = path.lastIndexOf('/');
			if (lastIndex > 0) {
				int index = 0;
				while (index < lastIndex) {
					index = path.indexOf('/', index + 1);
					if (index < 0)
						break;
					String parent = path.substring(0, index);
					File parentFile = mongoTemplate.findById(parent, File.class);
					if (parentFile == null) {
						parentFile = new File();
						parentFile.setPath(parent);
						parentFile.setDirectory(true);
						parentFile.setLastModified(System.currentTimeMillis());
						mongoTemplate.save(parentFile);
					} else if (!parentFile.isDirectory())
						throw new IOException("parent " + parent + " is not directory while writing path " + path);
				}
			}
			file = new File();
			file.setPath(path);
		} else if (file.isDirectory())
			throw new IOException("path " + path + " is directory,can not be written");
		try (InputStream ins = is; ByteArrayOutputStream os = new ByteArrayOutputStream(512 * 1024)) {
			IOUtils.copy(is, os);
			file.setData(os.toByteArray());
			file.setLastModified(System.currentTimeMillis());
		}
		mongoTemplate.save(file);
	}

	@Override
	public InputStream open(String path) throws IOException {
		path = normalizePath(path);
		if (path.equals("/"))
			return null;
		File file = mongoTemplate.findById(path, File.class);
		if (file == null)
			return null;
		if (file.isDirectory())
			return null;
		return new ByteArrayInputStream(file.getData());
	}

	@Override
	public boolean mkdir(String path) {
		path = normalizePath(path);
		if (path.equals("/"))
			return true;
		path = StringUtils.trimTailSlash(path);
		File file = mongoTemplate.findById(path, File.class);
		if (file != null)
			return file.isDirectory();
		int lastIndex = path.lastIndexOf('/');
		if (lastIndex > 0) {
			int index = 0;
			while (index <= lastIndex) {
				index = path.indexOf('/', index + 1);
				if (index < 0)
					break;
				String parent = path.substring(0, index);
				File parentFile = mongoTemplate.findById(parent, File.class);
				if (parentFile == null) {
					parentFile = new File();
					parentFile.setPath(parent);
					parentFile.setDirectory(true);
					parentFile.setLastModified(System.currentTimeMillis());
					mongoTemplate.save(parentFile);
				} else if (!parentFile.isDirectory())
					return false;
			}
		}
		file = new File();
		file.setPath(path);
		file.setDirectory(true);
		file.setLastModified(System.currentTimeMillis());
		mongoTemplate.save(file);
		return true;
	}

	@Override
	public boolean delete(String path) {
		path = normalizePath(path);
		if (path.equals("/"))
			return false;
		path = StringUtils.trimTailSlash(path);
		File file = mongoTemplate.findById(path, File.class);
		if (file == null)
			return false;
		if (file.isDirectory()) {
			int size = mongoTemplate
					.find(new Query(where("path").regex("^" + path.replaceAll("\\.", "\\\\.") + "/.*")).limit(1),
							File.class)
					.size();
			if (size > 0)
				return false;
		}
		mongoTemplate.remove(file);
		return true;
	}

	@Override
	public long getLastModified(String path) {
		path = normalizePath(path);
		if (path.equals("/"))
			return -1;
		path = StringUtils.trimTailSlash(path);
		File file = mongoTemplate.findById(path, File.class);
		return file != null ? file.getLastModified() : -1;
	}

	@Override
	public boolean exists(String path) {
		path = normalizePath(path);
		if (path.equals("/"))
			return true;
		path = StringUtils.trimTailSlash(path);
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
		path = normalizePath(path);
		if (path.equals("/"))
			return true;
		path = StringUtils.trimTailSlash(path);
		File file = mongoTemplate.findById(path, File.class);
		return file != null && file.isDirectory();
	}

	@Override
	public List<String> listFiles(String path) {
		path = normalizePath(path);
		if (!"/".equals(path)) {
			File file = mongoTemplate.findById(path, File.class);
			if (file == null || !file.isDirectory())
				return Collections.emptyList();
		}
		List<String> list = new ArrayList<>();
		String regex = "^" + path.replaceAll("\\.", "\\\\.") + (path.endsWith("/") ? "" : "/") + "[^/]*$";
		List<File> files = mongoTemplate.find(new Query(where("path").regex(regex)), File.class);
		for (File f : files) {
			if (f.isDirectory())
				continue;
			String name = f.getPath();
			list.add(name.substring(name.lastIndexOf('/') + 1));
			if (list.size() > MAX_PAGE_SIZE)
				throw new LimitExceededException("Exceed max size:" + MAX_PAGE_SIZE);
		}
		list.sort(null);
		return list;
	}

	@Override
	public List<FileInfo> listFilesAndDirectory(String path) {
		path = normalizePath(path);
		final List<FileInfo> list = new ArrayList<>();
		if (!"/".equals(path)) {
			File file = mongoTemplate.findById(path, File.class);
			if (file == null || !file.isDirectory())
				return Collections.emptyList();
		}
		String regex = "^" + path.replaceAll("\\.", "\\\\.") + (path.endsWith("/") ? "" : "/") + "[^/]*$";
		List<File> files = mongoTemplate.find(new Query(where("path").regex(regex)), File.class);
		for (File f : files) {
			String name = f.getPath();
			list.add(new FileInfo(name.substring(name.lastIndexOf('/') + 1), !f.isDirectory()));
			if (list.size() > MAX_PAGE_SIZE)
				throw new LimitExceededException("Exceed max size:" + MAX_PAGE_SIZE);
		}
		list.sort(COMPARATOR);
		return list;
	}

	private String normalizePath(String path) {
		if (!path.startsWith("/"))
			path = "/" + path;
		return FileUtils.normalizePath(path);
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
