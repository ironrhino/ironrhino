package org.ironrhino.security.oauth.server.domain;

import java.io.Serializable;

import org.ironrhino.security.oauth.server.enums.GrantType;

import lombok.Data;

@Data
public class OAuthAuthorization implements Serializable {

	private static final long serialVersionUID = 8659734973845517719L;

	private String accessToken;

	private String grantor;

	private String scope;

	private String code;

	private int lifetime;

	private String refreshToken;

	private GrantType grantType;

	private String address;

	private String deviceId;

	private String deviceName;

	private int expiresIn;

	private String clientId;

	private String clientName;

	private String clientOwner;

	private boolean kicked;

}
