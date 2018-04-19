package org.ironrhino.core.jdbc;

import java.io.Serializable;

import javax.persistence.Id;

import lombok.Data;

@Data
public class Dog implements Serializable {

	private static final long serialVersionUID = 5793007404230553394L;

	@Id
	private Integer id;

	private String name;

}
