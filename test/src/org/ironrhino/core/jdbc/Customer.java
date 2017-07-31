package org.ironrhino.core.jdbc;

import java.io.Serializable;

import lombok.Data;

@Data
public class Customer implements Serializable {

	private static final long serialVersionUID = -2436686091362486326L;

	private String identifyNo;

	private String name;

}
