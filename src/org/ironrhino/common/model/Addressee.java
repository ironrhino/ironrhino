package org.ironrhino.common.model;

import java.io.Serializable;

import lombok.Data;

@Data
public class Addressee implements Serializable {

	private static final long serialVersionUID = 5828814302567000566L;

	private String name;

	private String address;

	private String postcode;

	private String phone;

	private String description;

}
