package org.ironrhino.core.jdbc;

import java.io.Serializable;

public class Customer implements Serializable {

	private static final long serialVersionUID = -2436686091362486326L;
	
	private String identifyNo;
	
	private String name;

	public String getIdentifyNo() {
		return identifyNo;
	}

	public void setIdentifyNo(String identifyNo) {
		this.identifyNo = identifyNo;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
