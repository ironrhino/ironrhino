package org.ironrhino.rest;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.metadata.UiConfig;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties({ "localizedMessage", "cause", "stackTrace", "suppressed" })
public class RestStatus extends RuntimeException {

	private static final long serialVersionUID = -3866308675682764807L;

	public static final String CODE_OK = "0";

	public static final String CODE_REQUEST_TIMEOUT = "1";

	public static final String CODE_FORBIDDEN = "2";

	public static final String CODE_UNAUTHORIZED = "3";

	public static final String CODE_NOT_FOUND = "4";

	public static final String CODE_ALREADY_EXISTS = "5";

	public static final String CODE_FIELD_INVALID = "6";

	public static final String CODE_BAD_REQUEST = "7";

	public static final String CODE_INTERNAL_SERVER_ERROR = "-1";

	public static final RestStatus OK = valueOf(CODE_OK);
	public static final RestStatus REQUEST_TIMEOUT = valueOf(CODE_REQUEST_TIMEOUT, null, 408);
	public static final RestStatus FORBIDDEN = valueOf(CODE_FORBIDDEN, null, 403);
	public static final RestStatus UNAUTHORIZED = valueOf(CODE_UNAUTHORIZED, null, 401);
	public static final RestStatus NOT_FOUND = valueOf(CODE_NOT_FOUND, null, 404);

	@Getter
	@Setter
	@UiConfig(required = true)
	private String code;

	@Getter
	@Setter
	@UiConfig(required = true)
	private String status;

	@Getter
	@Setter
	private String message;

	@Getter
	@Setter
	@JsonIgnore
	private Integer httpStatusCode;

	protected RestStatus(String code, String status) {
		super(status);
		this.code = code;
		this.status = status;
	}

	protected RestStatus(String code, String status, String message) {
		super(message);
		this.code = code;
		this.status = status;
		this.message = message;
	}

	protected RestStatus(String code, String status, String message, Integer httpStatusCode) {
		super(message);
		this.code = code;
		this.status = status;
		this.message = message;
		this.httpStatusCode = httpStatusCode;
	}

	public static RestStatus valueOf(String code) {
		String status = findStatus(code);
		return new RestStatus(code, status);
	}

	public static RestStatus valueOf(String code, String message) {
		if (StringUtils.isBlank(message))
			return valueOf(code);
		String status = findStatus(code);
		return new RestStatus(code, status, message);
	}

	public static RestStatus valueOf(String code, String message, Integer httpStatusCode) {
		if (StringUtils.isBlank(message) && httpStatusCode == null)
			return valueOf(code);
		String status = findStatus(code);
		return new RestStatus(code, status, message, httpStatusCode);
	}

	protected static String findStatus(String code) {
		try {
			for (Field f : RestStatus.class.getDeclaredFields())
				if (Modifier.isStatic(f.getModifiers()) && Modifier.isFinal(f.getModifiers())
						&& f.getType() == String.class && f.get(null).equals(code)) {
					String status = f.getName();
					if (status.startsWith("CODE_"))
						status = status.substring(5);
					return status;
				}
		} catch (Exception e) {

		}
		return null;
	}

}
