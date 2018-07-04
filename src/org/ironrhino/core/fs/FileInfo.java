package org.ironrhino.core.fs;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FileInfo implements Serializable {

	private static final long serialVersionUID = 6556586903126364846L;

	private final String name;

	private final boolean file;

	private final long size;

	private final long lastModified;

	public FileInfo(String name, boolean file) {
		this(name, file, 0, 0);
	}

}
