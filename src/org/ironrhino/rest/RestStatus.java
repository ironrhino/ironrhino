package org.ironrhino.rest;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
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
	public static final RestStatus REQUEST_TIMEOUT = valueOf(CODE_REQUEST_TIMEOUT, "Request Timeout", 408);
	public static final RestStatus FORBIDDEN = valueOf(CODE_FORBIDDEN, "Forbidden", 403);
	public static final RestStatus UNAUTHORIZED = valueOf(CODE_UNAUTHORIZED, "Unauthorized", 401);
	public static final RestStatus NOT_FOUND = valueOf(CODE_NOT_FOUND, "Not Found", 404);

	static {
		StackTraceElement[] stackTrace = new StackTraceElement[0];
		OK.setStackTrace(stackTrace);
		REQUEST_TIMEOUT.setStackTrace(stackTrace);
		FORBIDDEN.setStackTrace(stackTrace);
		UNAUTHORIZED.setStackTrace(stackTrace);
		NOT_FOUND.setStackTrace(stackTrace);
	}

	@NotNull
	private final String code;

	@NotNull
	private final String status;

	private String message;

	private Map<String, List<String>> fieldErrors;

	@JsonIgnore
	private Integer httpStatusCode;

	protected RestStatus(String code, String status) {
		this(code, status, null);
	}

	protected RestStatus(String code, String status, String message) {
		this(code, status, message, null);
	}

	protected RestStatus(String code, String status, String message, Integer httpStatusCode) {
		super(message != null ? message : status);
		this.code = code;
		this.status = status;
		this.message = message;
		this.httpStatusCode = httpStatusCode;
	}

	public void addFieldError(String field, String error) {
		if (!this.code.equals(CODE_FIELD_INVALID))
			throw new IllegalStateException("Status should be FIELD_INVALID");
		if (fieldErrors == null)
			fieldErrors = new LinkedHashMap<>();
		List<String> errors = fieldErrors.get(field);
		if (errors == null) {
			errors = new ArrayList<>();
			fieldErrors.put(field, errors);
		}
		errors.add(error);
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
