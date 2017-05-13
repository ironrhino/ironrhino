package org.ironrhino.core.fs.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.ironrhino.core.fs.FileStorage;
import org.ironrhino.core.spring.configuration.ServiceImplementationConditional;
import org.ironrhino.core.util.DateUtils;
import org.ironrhino.core.util.ValueThenKeyComparator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.io.Files;

@Component("fileStorage")
@ServiceImplementationConditional(profiles = "ftp")
public class FtpFileStorage implements FileStorage {

	@Value("${fileStorage.uri:ftp://test:test@localhost}")
	protected URI uri;

	@Value("${fileStorage.baseUrl:}")
	protected String baseUrl;

	@Value("${ftp.controlEncoding:UTF-8}")
	protected String controlEncoding;

	@Value("${ftp.binaryMode:true}")
	protected boolean binaryMode;

	@Value("${ftp.passiveMode:true}")
	protected boolean passiveMode;

	private FTPClient openClient() throws IOException {
		FTPClient ftpClient = new FTPClient();
		ftpClient.setControlEncoding(controlEncoding);
		ftpClient.connect(uri.getHost(), uri.getPort() > 0 ? uri.getPort() : ftpClient.getDefaultPort());
		if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
			ftpClient.disconnect();
			throw new IOException("FTP server refused connection.");
		}
		String userInfo = uri.getUserInfo();
		boolean b;
		if (userInfo != null) {
			String[] arr = userInfo.split(":", 2);
			b = ftpClient.login(arr[0], arr.length > 1 ? arr[1] : null);
		} else {
			b = ftpClient.login("anonymous", "anonymous@test.com");
		}
		if (!b) {
			ftpClient.logout();
			throw new IllegalArgumentException("Illegal username or password");
		}

		ftpClient.setFileType(binaryMode ? FTP.BINARY_FILE_TYPE : FTP.ASCII_FILE_TYPE);
		if (passiveMode) {
			ftpClient.enterLocalPassiveMode();
		} else {
			ftpClient.enterLocalActiveMode();
		}
		return ftpClient;
	}

	private String getRealPath(String path, FTPClient ftpClient) throws IOException {
		return Files.simplifyPath(ftpClient.printWorkingDirectory() + uri.getPath() + path);
	}

	@Override
	public void write(InputStream is, String path) throws IOException {
		FTPClient ftpClient = null;
		try {
			ftpClient = openClient();
			String realPath = getRealPath(path, ftpClient);
			String chroot = ftpClient.printWorkingDirectory();
			String relativePath = realPath.substring(chroot.length() + 1);
			String[] arr = relativePath.split("/");
			StringBuilder sb = new StringBuilder(chroot);
			for (int i = 0; i < arr.length; i++) {
				sb.append("/").append(arr[i]);
				ftpClient.changeWorkingDirectory(sb.toString());
				if (ftpClient.getReplyCode() == 550) {
					ftpClient.makeDirectory(sb.toString());
				}
				if (i == arr.length - 2)
					break;
			}
			ftpClient.storeFile(realPath, is);
		} finally {
			IOUtils.closeQuietly(is);
			if (ftpClient != null) {
				if (ftpClient.isConnected())
					ftpClient.disconnect();
			}
		}
	}

	@Override
	public InputStream open(String path) throws IOException {
		FTPClient ftpClient = null;
		try {
			ftpClient = openClient();
			return ftpClient.retrieveFileStream(getRealPath(path, ftpClient));
		} finally {
			if (ftpClient != null) {
				if (ftpClient.isConnected())
					ftpClient.disconnect();
			}
		}
	}

	@Override
	public boolean mkdir(String path) throws IOException {
		FTPClient ftpClient = null;
		try {
			ftpClient = openClient();
			return ftpClient.makeDirectory(getRealPath(path, ftpClient));
		} finally {
			if (ftpClient != null) {
				if (ftpClient.isConnected())
					ftpClient.disconnect();
			}
		}
	}

	@Override
	public boolean delete(String path) throws IOException {
		FTPClient ftpClient = null;
		try {
			ftpClient = openClient();
			return ftpClient.deleteFile(getRealPath(path, ftpClient));
		} finally {
			if (ftpClient != null) {
				if (ftpClient.isConnected())
					ftpClient.disconnect();
			}
		}
	}

	@Override
	public long getLastModified(String path) throws IOException {
		FTPClient ftpClient = null;
		try {
			ftpClient = openClient();
			String modificationTime = ftpClient.getModificationTime(getRealPath(path, ftpClient));
			try {
				Date d = DateUtils.parse(modificationTime, "yyyyMMddHHmmss");
				return d.getTime() + TimeZone.getDefault().getRawOffset();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return -1;
		} finally {
			if (ftpClient != null) {
				if (ftpClient.isConnected())
					ftpClient.disconnect();
			}
		}
	}

	@Override
	public boolean exists(String path) throws IOException {
		try (InputStream is = open(path)) {
			boolean b = is != null;
			if (!b)
				b = isDirectory(path);
			return b;
		}
	}

	@Override
	public boolean rename(String fromPath, String toPath) throws IOException {
		FTPClient ftpClient = null;
		try {
			ftpClient = openClient();
			fromPath = getRealPath(fromPath, ftpClient);
			toPath = getRealPath(toPath, ftpClient);
			String s1 = fromPath.substring(0, fromPath.lastIndexOf('/'));
			String s2 = toPath.substring(0, fromPath.lastIndexOf('/'));
			if (!s1.equals(s2))
				return false;
			ftpClient.changeWorkingDirectory(s1);
			ftpClient.rename(fromPath.substring(fromPath.lastIndexOf('/') + 1),
					toPath.substring(toPath.lastIndexOf('/') + 1));
			return true;
		} finally {
			if (ftpClient != null) {
				if (ftpClient.isConnected())
					ftpClient.disconnect();
			}
		}

	}

	@Override
	public boolean isDirectory(String path) throws IOException {
		FTPClient ftpClient = null;
		try {
			ftpClient = openClient();
			ftpClient.changeWorkingDirectory(getRealPath(path, ftpClient));
			return ftpClient.getReplyCode() != 550;
		} finally {
			if (ftpClient != null) {
				if (ftpClient.isConnected())
					ftpClient.disconnect();
			}
		}
	}

	@Override
	public List<String> listFiles(String path) throws IOException {
		final List<String> list = new ArrayList<>();
		FTPClient ftpClient = null;
		try {
			ftpClient = openClient();
			for (FTPFile f : ftpClient.listFiles(getRealPath(path, ftpClient))) {
				if (f.isFile())
					list.add(f.getName());
			}
			return list;
		} finally {
			if (ftpClient != null) {
				if (ftpClient.isConnected())
					ftpClient.disconnect();
			}
		}

	}

	@Override
	public Map<String, Boolean> listFilesAndDirectory(String path) throws IOException {
		final Map<String, Boolean> map = new HashMap<>();
		FTPClient ftpClient = null;
		try {
			ftpClient = openClient();
			for (FTPFile f : ftpClient.listFiles(getRealPath(path, ftpClient))) {
				map.put(f.getName(), f.isFile());
			}
			List<Map.Entry<String, Boolean>> list = new ArrayList<>(map.entrySet());
			Collections.sort(list, comparator);
			Map<String, Boolean> sortedMap = new LinkedHashMap<>();
			for (Map.Entry<String, Boolean> entry : list)
				sortedMap.put(entry.getKey(), entry.getValue());
			return sortedMap;
		} finally {
			if (ftpClient != null) {
				if (ftpClient.isConnected())
					ftpClient.disconnect();
			}
		}
	}

	@Override
	public String getFileUrl(String path) {
		path = Files.simplifyPath(path);
		if (!path.startsWith("/"))
			path = '/' + path;
		return StringUtils.isNotBlank(baseUrl) ? baseUrl + path : path;
	}

	private ValueThenKeyComparator<String, Boolean> comparator = new ValueThenKeyComparator<String, Boolean>() {
		@Override
		protected int compareValue(Boolean a, Boolean b) {
			return b.compareTo(a);
		}
	};
}
