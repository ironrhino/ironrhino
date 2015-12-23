package org.ironrhino.security.oauth.server.domain;

import java.io.Serializable;

import org.ironrhino.security.oauth.server.enums.GrantType;

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

	private int expiresIn;

	private String clientId;

	private String clientName;

	private String clientOwner;

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getGrantor() {
		return grantor;
	}

	public void setGrantor(String grantor) {
		this.grantor = grantor;
	}

	public String getScope() {
		return scope;
	}

	public void setScope(String scope) {
		this.scope = scope;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public int getLifetime() {
		return lifetime;
	}

	public void setLifetime(int lifetime) {
		this.lifetime = lifetime;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public GrantType getGrantType() {
		return grantType;
	}

	public void setGrantType(GrantType grantType) {
		this.grantType = grantType;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public int getExpiresIn() {
		return expiresIn;
	}

	public void setExpiresIn(int expiresIn) {
		this.expiresIn = expiresIn;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getClientName() {
		return clientName;
	}

	public void setClientName(String clientName) {
		this.clientName = clientName;
	}

	public String getClientOwner() {
		return clientOwner;
	}

	public void setClientOwner(String clientOwner) {
		this.clientOwner = clientOwner;
	}

}
