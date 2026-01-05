package org.ironrhino.core.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor
@ToString
public class BusinessException extends RuntimeException {

	private static final long serialVersionUID = 4265063156880365173L;

	@Getter
	private final String code;

	@Getter
	private final String message;

}
