package org.ironrhino.core.fs.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.ironrhino.core.fs.FileInfo;
import org.ironrhino.core.spring.configuration.ServiceImplementationConditional;
import org.ironrhino.core.util.DateUtils;
import org.ironrhino.core.util.FileUtils;
import org.ironrhino.core.util.LimitExceededException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Component("fileStorage")
@ServiceImplementationConditional(profiles = "ftp")
@Slf4j
public class FtpFileStorage extends AbstractFileStorage {

	@Getter
	@Setter
	@Value("${fileStorage.uri:ftp://test:test@localhost}")
	protected URI uri;

	@Getter
	@Setter
	@Value("${ftp.workingDirectory:}")
	protected String workingDirectory;

	@Getter
	@Setter
	@Value("${ftp.dataTimeout:10000}")
	protected int dataTimeout = 10000;

	@Getter
	@Setter
	@Value("${ftp.controlEncoding:UTF-8}")
	protected String controlEncoding = "UTF-8";

	@Getter
	@Setter
	@Value("${ftp.binaryMode:true}")
	protected boolean binaryMode = true;

	@Getter
	@Setter
	@Value("${ftp.passiveMode:true}")
	protected boolean passiveMode = true;

	@Getter
	@Setter
	@Value("${ftp.bufferThreshold:1048576}")
	protected long bufferThreshold = 1048576;

	@Getter
	@Setter
	@Value("${ftp.pool.maxTotal:20}")
	protected int maxTotal = 20;

	@Getter
	@Setter
	@Value("${ftp.pool.maxIdle:5}")
	protected int maxIdle = 5;

	@Getter
	@Setter
	@Value("${ftp.pool.minIdle:1}")
	protected int minIdle = 1;

	@Getter
	@Setter
	@Value("${ftp.pool.maxWaitMillis:60000}")
	protected int maxWaitMillis = 60000;

	@Getter
	@Setter
	@Value("${ftp.pool.minEvictableIdleTimeMillis:300000}")
	protected int minEvictableIdleTimeMillis = 300000;

	private ObjectPool<FTPClient> pool;

	@PostConstruct
	public void init() {
		PooledObjectFactory<FTPClient> factory = new BasePooledObjectFactory<FTPClient>() {

			@Override
			public FTPClient create() throws Exception {
				FTPClient ftpClient = uri.getScheme().equals("ftps") ? new FTPSClient() : new FTPClient();
				ftpClient.setDataTimeout(dataTimeout);
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
							log.error(e.getMessage(), e);
					} finally {
						try {
							ftpClient.disconnect();
						} catch (FTPConnectionClosedException e) {
							// Ignore
						} catch (IOException e) {
							if (!e.getMessage().equals("Broken pipe"))
								log.error(e.getMessage(), e);
						}
					}
				}
			}
		};
		GenericObjectPoolConfig<FTPClient> poolConfig = new GenericObjectPoolConfig<>();
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
				String pathname = getPathname(path, ftpClient);
				String workingDirectory = ftpClient.printWorkingDirectory();
				workingDirectory = org.ironrhino.core.util.StringUtils.trimTailSlash(workingDirectory);
				String relativePath = pathname.substring(workingDirectory.length() + 1);
				String[] arr = relativePath.split("/");
				if (arr.length > 1) {
					StringBuilder sb = new StringBuilder(workingDirectory);
					for (int i = 0; i < arr.length - 1; i++) {
						sb.append("/").append(arr[i]);
						ftpClient.changeWorkingDirectory(sb.toString());
						if (ftpClient.getReplyCode() == 550) {
							ftpClient.makeDirectory(sb.toString());
						}
					}
				}
				ftpClient.storeFile(pathname, ins);
				return null;
			});
		}
	}

	@Override
	public InputStream open(String path) throws IOException {
		return execute(ftpClient -> {
			String pathname = getPathname(path, ftpClient);
			FTPFile[] files = ftpClient.listFiles(pathname);
			if (files == null || files.length != 1 || !files[0].isFile())
				return null;
			long size = files[0].getSize();
			if (size <= bufferThreshold) {
				// small file
				ByteArrayOutputStream bos = new ByteArrayOutputStream((int) size);
				ftpClient.retrieveFile(pathname, bos);
				return new ByteArrayInputStream(bos.toByteArray());
			}
			return new ProxyInputStream(ftpClient.retrieveFileStream(pathname)) {

				private AtomicBoolean closed = new AtomicBoolean();

				@Override
				public void close() throws IOException {
					if (!closed.compareAndSet(false, true))
						return;
					try {
						super.close();
					} finally {
						try {
							ftpClient.completePendingCommand();
							pool.returnObject(ftpClient);
						} catch (Exception e) {
							try {
								pool.invalidateObject(ftpClient);
							} catch (Exception e1) {
								log.error(e1.getMessage(), e1);
							}
							log.error(e.getMessage(), e);
						}
					}
				}
			};
		});
	}

	@Override
	public boolean mkdir(String path) {
		return executeWrapped(ftpClient -> {
			String pathname = getPathname(path, ftpClient);
			String workingDirectory = ftpClient.printWorkingDirectory();
			workingDirectory = org.ironrhino.core.util.StringUtils.trimTailSlash(workingDirectory);
			String relativePath = pathname.substring(workingDirectory.length() + 1);
			String[] arr = relativePath.split("/");
			StringBuilder sb = new StringBuilder(workingDirectory);
			for (int i = 0; i < arr.length; i++) {
				sb.append("/").append(arr[i]);
				ftpClient.changeWorkingDirectory(sb.toString());
				if (ftpClient.getReplyCode() == 550) {
					if (!ftpClient.makeDirectory(sb.toString()))
						return false;
				}
			}
			return true;
		});
	}

	@Override
	public boolean delete(String path) {
		return executeWrapped(ftpClient -> {
			String pathname = getPathname(path, ftpClient);
			ftpClient.changeWorkingDirectory(pathname);
			if (ftpClient.getReplyCode() != 550) {
				ftpClient.changeToParentDirectory();
				return ftpClient.removeDirectory(pathname);
			} else {
				return ftpClient.deleteFile(pathname);
			}
		});
	}

	@Override
	public long getLastModified(String path) {
		return executeWrapped(ftpClient -> {
			String modificationTime = ftpClient.getModificationTime(getPathname(path, ftpClient));
			if (modificationTime != null)
				try {
					Date d = DateUtils.parse(modificationTime, "yyyyMMddHHmmss");
					return d.getTime() + TimeZone.getDefault().getRawOffset();
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			return 0L;
		});

	}

	@Override
	public boolean exists(String path) {
		boolean isFile = executeWrapped(ftpClient -> {
			return ftpClient.getModificationTime(getPathname(path, ftpClient)) != null;
		});
		return isFile || isDirectory(path);
	}

	@Override
	public boolean rename(String fromPath, String toPath) {
		return executeWrapped(ftpClient -> {
			String _fromPath = getPathname(fromPath, ftpClient);
			String _toPath = getPathname(toPath, ftpClient);
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
	public boolean isDirectory(String path) {
		if (path.equals("") || path.equals("/"))
			return true;
		return executeWrapped(ftpClient -> {
			ftpClient.changeWorkingDirectory(getPathname(path, ftpClient));
			return ftpClient.getReplyCode() != 550;
		});
	}

	@Override
	public List<FileInfo> listFiles(String path) {
		List<FileInfo> result = executeWrapped(ftpClient -> {
			List<FileInfo> list = new ArrayList<>();
			for (FTPFile f : ftpClient.listFiles(getPathname(path, ftpClient))) {
				if (f.isFile())
					list.add(new FileInfo(f.getName(), true, f.getSize(), f.getTimestamp().getTimeInMillis()));
				if (list.size() > MAX_PAGE_SIZE)
					throw new LimitExceededException("Exceed max size:" + MAX_PAGE_SIZE);
			}
			return list;
		});
		result.sort(COMPARATOR);
		return result;
	}

	@Override
	public List<FileInfo> listFilesAndDirectory(String path) {
		List<FileInfo> result = executeWrapped(ftpClient -> {
			final List<FileInfo> list = new ArrayList<>();
			for (FTPFile f : ftpClient.listFiles(getPathname(path, ftpClient)))
				list.add(new FileInfo(f.getName(), f.isFile(), f.getSize(), f.getTimestamp().getTimeInMillis()));
			if (list.size() > MAX_PAGE_SIZE)
				throw new LimitExceededException("Exceed max size:" + MAX_PAGE_SIZE);
			return list;
		});
		result.sort(COMPARATOR);
		return result;
	}

	private String getPathname(String path, FTPClient ftpClient) throws IOException {
		if (!path.startsWith("/"))
			path = "/" + path;
		String wd = StringUtils.isBlank(workingDirectory) ? ftpClient.printWorkingDirectory() : workingDirectory;
		return FileUtils.normalizePath(wd + uri.getPath() + path);
	}

	protected <T> T executeWrapped(Callback<T> callback) {
		try {
			return execute(callback);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public <T> T execute(Callback<T> callback) throws IOException {
		FTPClient ftpClient = null;
		boolean deferReturn = false;
		try {
			ftpClient = pool.borrowObject();
			String workingDirectory = ftpClient.printWorkingDirectory();
			T val = callback.doWithFTPClient(ftpClient);
			if (!(val instanceof ProxyInputStream)) {
				ftpClient.changeWorkingDirectory(workingDirectory);
			} else {
				deferReturn = true;
			}
			return val;
		} catch (IOException e) {
			if (ftpClient != null)
				try {
					pool.invalidateObject(ftpClient);
					ftpClient = null;
				} catch (Exception ex) {
					log.error(ex.getMessage(), ex);
				}
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if (ftpClient != null && !deferReturn)
				try {
					pool.returnObject(ftpClient);
				} catch (Exception ex) {
					log.error(ex.getMessage(), ex);
				}
		}
	}

	public static interface Callback<T> {
		public T doWithFTPClient(FTPClient ftpClient) throws IOException;
	}

	static abstract class ProxyInputStream extends FilterInputStream {
		public ProxyInputStream(InputStream proxy) {
			super(proxy);
		}

		@Override
		public int read() throws IOException {
			try {
				beforeRead(1);
				int b = in.read();
				afterRead(b != -1 ? 1 : -1);
				return b;
			} catch (IOException e) {
				handleIOException(e);
			}
			return -1;
		}

		@Override
		public int read(byte[] bts) throws IOException {
			try {
				beforeRead(bts != null ? bts.length : 0);
				int n = in.read(bts);
				afterRead(n);
				return n;
			} catch (IOException e) {
				handleIOException(e);
			}
			return -1;
		}

		@Override
		public int read(byte[] bts, int off, int len) throws IOException {
			try {
				beforeRead(len);
				int n = in.read(bts, off, len);
				afterRead(n);
				return n;
			} catch (IOException e) {
				handleIOException(e);
			}
			return -1;
		}

		@Override
		public long skip(long ln) throws IOException {
			try {
				return in.skip(ln);
			} catch (IOException e) {
				handleIOException(e);
			}
			return 0L;
		}

		@Override
		public int available() throws IOException {
			try {
				return super.available();
			} catch (IOException e) {
				handleIOException(e);
			}
			return 0;
		}

		@Override
		public void close() throws IOException {
			try {
				in.close();
			} catch (IOException e) {
				handleIOException(e);
			}
		}

		@Override
		public synchronized void mark(int readlimit) {
			in.mark(readlimit);
		}

		@Override
		public synchronized void reset() throws IOException {
			try {
				in.reset();
			} catch (IOException e) {
				handleIOException(e);
			}
		}

		@Override
		public boolean markSupported() {
			return in.markSupported();
		}

		protected void beforeRead(int n) throws IOException {
		}

		protected void afterRead(int n) throws IOException {
		}

		protected void handleIOException(IOException e) throws IOException {
			throw e;
		}
	}

}
