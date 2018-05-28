package org.ironrhino.core.dataroute;

import java.io.Serializable;

import lombok.Data;

@Data
public class Ownership implements Serializable {

	private static final long serialVersionUID = -6631915278788244678L;

	private String name;

	private String owner;

}
