package org.ironrhino.core.dataroute;

import java.io.Serializable;

import lombok.Data;

@Data
public class Pet implements Serializable {

	private static final long serialVersionUID = -6631915278788244678L;

	private String name;

}
