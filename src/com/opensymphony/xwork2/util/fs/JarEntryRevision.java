
package com.opensymphony.xwork2.util.fs;

import java.net.URL;

import com.opensymphony.xwork2.FileManager;

public class JarEntryRevision extends Revision {

	protected URL jarFileURL;
	protected long lastModified;

	public static Revision build(URL fileUrl, FileManager fileManager) {
		return new JarEntryRevision(fileUrl, -1);
	}

	private JarEntryRevision(URL jarFileURL, long lastModified) {
		if (jarFileURL == null) {
			throw new IllegalArgumentException("jarFileURL cannot be null");
		}
		this.jarFileURL = jarFileURL;
		this.lastModified = lastModified;
	}

	public boolean needsReloading() {
		return false;
	}

}