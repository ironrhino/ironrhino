package org.ironrhino.batch.tasklet.ftp;

import java.io.IOException;
import java.net.URI;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.ironrhino.core.util.CheckedFunction;
import org.ironrhino.core.util.FileUtils;
import org.springframework.batch.core.step.tasklet.Tasklet;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Setter
@Slf4j
public abstract class AbstractFtpTask implements Tasklet {

	protected URI uri;

	protected String workingDirectory;

	protected int defaultTimeout = 10000;

	protected int dataTimeout = 10000;

	protected String controlEncoding = "UTF-8";

	protected boolean binaryMode = true;

	protected boolean passiveMode = true;

	protected <T> T execute(CheckedFunction<FTPClient, T, IOException> callback) throws Exception {
		FTPClient client = null;
		try {
			client = createClient();
			return callback.apply(client);
		} finally {
			if (client != null) {
				if (client.isConnected()) {
					try {
						client.logout();
					} catch (FTPConnectionClosedException e) {
						// Ignore
					} catch (IOException e) {
						if (!e.getMessage().contains("Broken pipe"))
							log.error(e.getMessage(), e);
					} finally {
						try {
							client.disconnect();
						} catch (FTPConnectionClosedException e) {
							// Ignore
						} catch (IOException e) {
							if (!e.getMessage().contains("Broken pipe"))
								log.error(e.getMessage(), e);
						}
					}
				}
			}
		}
	}

	protected FTPClient createClient() throws Exception {
		FTPClient ftpClient = uri.getScheme().equals("ftps") ? new FTPSClient() : new FTPClient();
		ftpClient.setDefaultTimeout(defaultTimeout);
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

	protected String getPathname(String path, FTPClient ftpClient) throws IOException {
		if (!path.startsWith("/"))
			path = "/" + path;
		String wd = StringUtils.isBlank(workingDirectory) ? ftpClient.printWorkingDirectory() : workingDirectory;
		return FileUtils.normalizePath(wd + uri.getPath() + path);
	}

}
