package org.ironrhino.common.util;

import java.io.Serializable;

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

	public String getFirstArea() {
		return firstArea;
	}

	public void setFirstArea(String firstArea) {
		this.firstArea = firstArea;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getSecondArea() {
		return secondArea;
	}

	public void setSecondArea(String secondArea) {
		this.secondArea = secondArea;
	}

	public String getThirdArea() {
		return thirdArea;
	}

	public void setThirdArea(String thirdArea) {
		this.thirdArea = thirdArea;
	}

	@Override
	public String toString() {
		return location;
	}

}
