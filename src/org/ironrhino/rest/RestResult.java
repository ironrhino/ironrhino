package org.ironrhino.rest;

import java.io.Serializable;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RestResult<T> implements Serializable {

	private static final long serialVersionUID = -6095457767997229847L;

	private final String code;

	private final String status;

	private final T data;

	public static <T> RestResult<T> of(T data) {
		return new RestResult<>("0", "OK", data);
	}

}
