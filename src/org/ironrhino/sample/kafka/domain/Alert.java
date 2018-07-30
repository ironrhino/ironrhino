package org.ironrhino.sample.kafka.domain;

import java.io.Serializable;

import lombok.Data;

@Data
public class Alert implements Serializable {

	private static final long serialVersionUID = -5247602843673308841L;

	private String id;

	private String content;

	@Override
	public String toString() {
		return this.getId();
	}

}
