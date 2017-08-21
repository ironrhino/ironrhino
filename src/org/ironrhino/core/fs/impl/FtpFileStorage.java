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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.io.input.ProxyInputStream;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.ironrhino.core.spring.configuration.ServiceImplementationConditional;
import org.ironrhino.core.util.DateUtils;
import org.ironrhino.core.util.FileUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Primary
@Component("fileStorage")
@ServiceImplementationConditional(profiles = "ftp")
public class FtpFileStorage extends AbstractFileStorage {

	@Autowired
	private Logger logger;

	@Getter
	@Setter
	@Value("${fileStorage.uri:ftp://test:test@localhost}")
	protected URI uri;

	@Getter
	@Setter
	@Value("${ftp.controlEncoding:UTF-8}")
	protected String controlEncoding;

	@Getter
	@Setter
	@Value("${ftp.binaryMode:true}")
	protected boolean binaryMode;

	@Getter
	@Setter
	@Value("${ftp.passiveMode:true}")
	protected boolean passiveMode;

	@Getter
	@Setter
	@Value("${ftp.pool.maxTotal:20}")
	protected int maxTotal;

	@Getter
	@Setter
	@Value("${ftp.pool.maxIdle:5}")
	protected int maxIdle;

	@Getter
	@Setter
	@Value("${ftp.pool.minIdle:1}")
	protected int minIdle;

	@Getter
	@Setter
	@Value("${ftp.pool.maxWaitMillis:60000}")
	protected int maxWaitMillis;

	@Getter
	@Setter
	@Value("${ftp.pool.minEvictableIdleTimeMillis:300000}")
	protected int minEvictableIdleTimeMillis;

	private ObjectPool<FTPClient> pool;

	@PostConstruct
	public void init() {
		PooledObjectFactory<FTPClient> factory = new BasePooledObjectFactory<FTPClient>() {

			@Override
			public FTPClient create() throws Exception {
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
					throw new IllegalArgumentException("Invalid username or password");
				}
				ftpClient.setFileType(binaryMode ? FTP.BINARY_FILE_TYPE : FTP.ASCII_FILE_TYPE);
				if (passiveMode) {
					ftpClient.enterLocalPassiveMode();
				} else {
					ftpClient.enterLocalActiveMode();
				}
				return ftpClient;
			}

			@Override
			public PooledObject<FTPClient> wrap(FTPClient ftpClient) {
				return new DefaultPooledObject<>(ftpClient);
			}

			@Override
			public boolean validateObject(PooledObject<FTPClient> po) {
				FTPClient ftpClient = po.getObject();
				try {
					return ftpClient.sendNoOp() && ftpClient.printWorkingDirectory() != null;
				} catch (IOException e) {
					return false;
				}
			}

			@Override
			public void destroyObject(PooledObject<FTPClient> po) {
				FTPClient ftpClient = po.getObject();
				if (ftpClient.isConnected()) {
					try {
						ftpClient.logout();
					} catch (FTPConnectionClosedException e) {
						// Ignore
					} catch (IOException e) {
						if (!e.getMessage().equals("Broken pipe"))
							logger.error(e.getMessage(), e);
					} finally {
						try {
							ftpClient.disconnect();
						} catch (FTPConnectionClosedException e) {
							// Ignore
						} catch (IOException e) {
							if (!e.getMessage().equals("Broken pipe"))
								logger.error(e.getMessage(), e);
						}
					}
				}
			}
		};
		GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
		poolConfig.setMaxTotal(maxTotal);
		poolConfig.setMaxIdle(maxIdle);
		poolConfig.setMinIdle(minIdle);
		poolConfig.setMaxWaitMillis(maxWaitMillis);
		poolConfig.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
		poolConfig.setSoftMinEvictableIdleTimeMillis(poolConfig.getMinEvictableIdleTimeMillis());
		poolConfig.setLifo(false);
		poolConfig.setTestOnBorrow(true);
		pool = new GenericObjectPool<FTPClient>(factory, poolConfig);
	}

	@PreDestroy
	public void destroy() {
		pool.close();
	}

	@Override
	public void write(InputStream is, String path) throws IOException {
		try (InputStream ins = is) {
			execute(ftpClient -> {
				String realPath = getRealPath(path, ftpClient);
				String workingDirectory = ftpClient.printWorkingDirectory();
				workingDirectory = org.ironrhino.core.util.StringUtils.trimTailSlash(workingDirectory);
				String relativePath = realPath.substring(workingDirectory.length() + 1);
				String[] arr = relativePath.split("/");
				StringBuilder sb = new StringBuilder(workingDirectory);
				for (int i = 0; i < arr.length; i++) {
					sb.append("/").append(arr[i]);
					ftpClient.changeWorkingDirectory(sb.toString());
					if (ftpClient.getReplyCode() == 550) {
						ftpClient.makeDirectory(sb.toString());
					}
					if (i == arr.length - 2)
						break;
				}
				ftpClient.storeFile(realPath, ins);
				return null;
			});
		}
	}

	@Override
	public InputStream open(String path) throws IOException {
		return execute(ftpClient -> {
			InputStream is = ftpClient.retrieveFileStream(getRealPath(path, ftpClient));
			if (is == null)
				return null;
			return new ProxyInputStream(is) {

				private final Object closeLock = new Object();
				private volatile boolean closed = false;

				@Override
				public void close() throws IOException {
					synchronized (closeLock) {
						if (closed) {
							return;
						}
						closed = true;
					}
					super.close();
					ftpClient.completePendingCommand();
					try {
						pool.returnObject(ftpClient);
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
					}
				}
			};
		});
	}

	@Override
	public boolean mkdir(String path) throws IOException {
		return execute(ftpClient -> {
			return ftpClient.makeDirectory(getRealPath(path, ftpClient));
		});
	}

	@Override
	public boolean delete(String path) throws IOException {
		return execute(ftpClient -> {
			return ftpClient.deleteFile(getRealPath(path, ftpClient));
		});
	}

	@Override
	public long getLastModified(String path) throws IOException {
		return execute(ftpClient -> {
			String modificationTime = ftpClient.getModificationTime(getRealPath(path, ftpClient));
			if (modificationTime != null)
				try {
					Date d = DateUtils.parse(modificationTime, "yyyyMMddHHmmss");
					return d.getTime() + TimeZone.getDefault().getRawOffset();
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			return -1L;
		});

	}

	@Override
	public boolean exists(String path) throws IOException {
		boolean isFile = execute(ftpClient -> {
			return ftpClient.getModificationTime(getRealPath(path, ftpClient)) != null;
		});
		return isFile || isDirectory(path);
	}

	@Override
	public boolean rename(String fromPath, String toPath) throws IOException {
		return execute(ftpClient -> {
			String _fromPath = getRealPath(fromPath, ftpClient);
			String _toPath = getRealPath(toPath, ftpClient);
			String s1 = _fromPath.substring(0, _fromPath.lastIndexOf('/'));
			String s2 = _toPath.substring(0, _toPath.lastIndexOf('/'));
			if (!s1.equals(s2))
				return false;
			ftpClient.changeWorkingDirectory(s1);
			ftpClient.rename(_fromPath.substring(_fromPath.lastIndexOf('/') + 1),
					_toPath.substring(_toPath.lastIndexOf('/') + 1));
			return true;
		});

	}

	@Override
	public boolean isDirectory(String path) throws IOException {
		return execute(ftpClient -> {
			ftpClient.changeWorkingDirectory(getRealPath(path, ftpClient));
			return ftpClient.getReplyCode() != 550;
		});
	}

	@Override
	public List<String> listFiles(String path) throws IOException {
		return execute(ftpClient -> {
			List<String> list = new ArrayList<>();
			for (FTPFile f : ftpClient.listFiles(getRealPath(path, ftpClient))) {
				if (f.isFile())
					list.add(f.getName());
			}
			return list;
		});

	}

	@Override
	public Map<String, Boolean> listFilesAndDirectory(String path) throws IOException {
		return execute(ftpClient -> {
			final Map<String, Boolean> map = new HashMap<>();
			for (FTPFile f : ftpClient.listFiles(getRealPath(path, ftpClient))) {
				map.put(f.getName(), f.isFile());
			}
			List<Map.Entry<String, Boolean>> list = new ArrayList<>(map.entrySet());
			Collections.sort(list, comparator);
			Map<String, Boolean> sortedMap = new LinkedHashMap<>();
			for (Map.Entry<String, Boolean> entry : list)
				sortedMap.put(entry.getKey(), entry.getValue());
			return sortedMap;
		});
	}

	private String getRealPath(String path, FTPClient ftpClient) throws IOException {
		return FileUtils.normalizePath(ftpClient.printWorkingDirectory() + uri.getPath() + path);
	}

	public <T> T execute(Callback<T> callback) throws IOException {
		FTPClient ftpClient = null;
		boolean deferReturn = false;
		try {
			ftpClient = pool.borrowObject();
			String workingDirectory = ftpClient.printWorkingDirectory();
			T val = callback.doWithFTPClient(ftpClient);
			if (!(val instanceof InputStream)) {
				ftpClient.changeWorkingDirectory(workingDirectory);
			} else {
				deferReturn = true;
			}
			return val;
		} catch (IOException e) {
			if (ftpClient != null)
				try {
					pool.invalidateObject(ftpClient);
				} catch (Exception ex) {
					logger.error(ex.getMessage(), ex);
				}
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if (ftpClient != null && !deferReturn)
				try {
					pool.returnObject(ftpClient);
				} catch (Exception ex) {
					logger.error(ex.getMessage(), ex);
				}
		}
	}

	public static interface Callback<T> {
		public T doWithFTPClient(FTPClient ftpClient) throws IOException;
	}
}
