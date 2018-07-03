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

}
