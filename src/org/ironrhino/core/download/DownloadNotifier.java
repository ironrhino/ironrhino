package org.ironrhino.core.download;

import org.springframework.security.core.userdetails.UserDetails;

@FunctionalInterface
public interface DownloadNotifier {

	void notify(UserDetails operator, String fileName, String password, UserDetails doubleChecker);

}
