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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.ironrhino.core.spring.configuration.ServiceImplementationConditional;
import org.ironrhino.core.util.FileUtils;
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
		path = FileUtils.normalizePath(path);
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
		path = FileUtils.normalizePath(path);
		if (path.equals("/"))
			throw new IOException("cannot direct access path /");
		File file = mongoTemplate.findById(path, File.class);
		if (file == null)
			throw new IOException("path " + path + " doesn't exist");
		if (file.isDirectory())
			throw new IOException("path " + path + " is directory");
		return new ByteArrayInputStream(file.getData());
	}

	@Override
	public boolean mkdir(String path) {
		path = FileUtils.normalizePath(path);
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
		path = FileUtils.normalizePath(path);
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
		path = FileUtils.normalizePath(path);
		if (path.equals("/"))
			return -1;
		path = StringUtils.trimTailSlash(path);
		File file = mongoTemplate.findById(path, File.class);
		return file != null ? file.getLastModified() : -1;
	}

	@Override
	public boolean exists(String path) {
		path = FileUtils.normalizePath(path);
		if (path.equals("/"))
			return true;
		path = StringUtils.trimTailSlash(path);
		return mongoTemplate.findById(path, File.class) != null;
	}

	@Override
	public boolean rename(String fromPath, String toPath) {
		fromPath = FileUtils.normalizePath(fromPath);
		toPath = FileUtils.normalizePath(toPath);
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
		path = FileUtils.normalizePath(path);
		if (path.equals("/"))
			return true;
		path = StringUtils.trimTailSlash(path);
		File file = mongoTemplate.findById(path, File.class);
		return file != null && file.isDirectory();
	}

	@Override
	public List<String> listFiles(String path) {
		path = FileUtils.normalizePath(path);
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
		}
		Collections.sort(list);
		return list;
	}

	@Override
	public Map<String, Boolean> listFilesAndDirectory(String path) {
		path = FileUtils.normalizePath(path);
		final Map<String, Boolean> map = new HashMap<>();
		if (!"/".equals(path)) {
			File file = mongoTemplate.findById(path, File.class);
			if (file == null || !file.isDirectory())
				return Collections.emptyMap();
		}
		String regex = "^" + path.replaceAll("\\.", "\\\\.") + (path.endsWith("/") ? "" : "/") + "[^/]*$";
		List<File> files = mongoTemplate.find(new Query(where("path").regex(regex)), File.class);
		for (File f : files) {
			String name = f.getPath();
			map.put(name.substring(name.lastIndexOf('/') + 1), !f.isDirectory());
		}
		List<Map.Entry<String, Boolean>> list = new ArrayList<>(map.entrySet());
		Collections.sort(list, COMPARATOR);
		Map<String, Boolean> sortedMap = new LinkedHashMap<>();
		for (Map.Entry<String, Boolean> entry : list)
			sortedMap.put(entry.getKey(), entry.getValue());
		return sortedMap;
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
