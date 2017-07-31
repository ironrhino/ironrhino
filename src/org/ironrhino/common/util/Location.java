package org.ironrhino.common.util;

import java.io.Serializable;

import lombok.Data;

@Data
public class Location implements Serializable {

	private static final long serialVersionUID = 3776595451619779358L;

	private String location;

	private String firstArea;

	private String secondArea;

	private String thirdArea;

	public Location() {

	}

	public Location(String location) {
		this.location = location;
	}

	@Override
	public String toString() {
		return location;
	}

}
