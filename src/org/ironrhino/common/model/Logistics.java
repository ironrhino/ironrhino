package org.ironrhino.common.model;

import java.io.Serializable;

import lombok.Data;

@Data
public class Logistics implements Serializable {

	private static final long serialVersionUID = 5828814308567000566L;

	private String type;

	private String provider;

	private String invoiceNo;

	private String description;

}
