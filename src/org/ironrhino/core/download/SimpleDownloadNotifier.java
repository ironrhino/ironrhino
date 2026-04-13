package org.ironrhino.core.download;

import org.springframework.security.core.userdetails.UserDetails;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SimpleDownloadNotifier implements DownloadNotifier {

	@Override
	public void notify(UserDetails operator, String fileName, String password, UserDetails doubleChecker) {
		StringBuilder sb = new StringBuilder();
		sb.append(operator).append(" downloaded file ").append(fileName);
		if (password != null)
			sb.append(" encrypted by ").append(password);
		if (doubleChecker != null)
			sb.append(" authorized by ").append(doubleChecker);
		log.info(sb.toString());
	}

}
