package org.ironrhino.core.fs;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class FileInfo implements Serializable {

	private static final long serialVersionUID = 6556586903126364846L;

	String name;

	boolean file;

	long size;

	long lastModified;

	public FileInfo(String name, boolean file) {
		this(name, file, 0, 0);
	}

}
