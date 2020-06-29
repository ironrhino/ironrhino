package org.ironrhino.security.oauth.server.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@RequiredArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({ "localizedMessage", "cause", "stackTrace", "suppressed" })
public class OAuthError extends RuntimeException {

	private static final long serialVersionUID = 8659734973845517719L;

	public static final String INVALID_REQUEST = "invalid_request";
	public static final String INVALID_CLIENT = "invalid_client";
	public static final String INVALID_TOKEN = "invalid_token";
	public static final String INVALID_GRANT = "invalid_grant";
	public static final String INVALID_SCOPE = "invalid_scope";
	public static final String INSUFFICIENT_SCOPE = "insufficient_scope";
	public static final String UNAUTHORIZED_CLIENT = "unauthorized_client";
	public static final String UNSUPPORTED_GRANT_TYPE = "unsupported_grant_type";

	// error message
	public static final String ERROR_MISSING_TOKEN = "missing_token";
	public static final String ERROR_EXPIRED_TOKEN = "expired_token";
	public static final String ERROR_KICKED_TOKEN = "kicked_token";
	public static final String ERROR_INVALID_USER = "invalid_user";

	private final String error;

	@JsonProperty("error_message")
	private String errorMessage;

	@JsonProperty("error_uri")
	private String errorUri;

	public OAuthError(String error, String errorMessage) {
		super(errorMessage == null ? error : error + ": " + errorMessage);
		this.error = error;
		this.errorMessage = errorMessage;
	}

}
