package org.ironrhino.sample.api;

import java.lang.reflect.Field;

import javassist.Modifier;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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

	public static final String CODE_INTERNAL_SERVER_ERROR = "-1";

	public static final RestStatus OK = valueOf(CODE_OK);
	public static final RestStatus REQUEST_TIMEOUT = valueOf(CODE_REQUEST_TIMEOUT);
	public static final RestStatus FORBIDDEN = valueOf(CODE_FORBIDDEN);
	public static final RestStatus UNAUTHORIZED = valueOf(CODE_UNAUTHORIZED);
	public static final RestStatus NOT_FOUND = valueOf(CODE_NOT_FOUND);

	private String code;

	private String status;

	private String message;

	public RestStatus(String code, String status) {
		super(status);
		this.code = code;
		this.status = status;
	}

	public RestStatus(String code, String status, String message) {
		super(message);
		this.code = code;
		this.status = status;
		this.message = message;
	}

	public String getCode() {
		return code;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getMessage() {
		return message;
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

	private static String findStatus(String code) {
		try {
			for (Field f : RestStatus.class.getDeclaredFields())
				if (Modifier.isStatic(f.getModifiers())
						&& Modifier.isFinal(f.getModifiers())
						&& f.getType() == String.class
						&& f.get(null).equals(code)) {
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
