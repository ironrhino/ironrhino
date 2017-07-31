package org.ironrhino.common.model;

import java.io.Serializable;

import lombok.Data;

@Data
public class SimpleMessage implements Serializable {

	private static final long serialVersionUID = -4969074449395319492L;

	private String from;

	private String to;

	private String subject;

	private String content;

}
