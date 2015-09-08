package org.ironrhino.rest.client.token;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Token implements java.io.Serializable {

	private static final long serialVersionUID = 3664222731669918663L;

	@JsonProperty("access_token")
	private String accessToken;

	@JsonProperty("expires_in")
	private int expiresIn;

	@JsonProperty("refresh_token")
	private String refreshToken;

	@JsonIgnore
	private long createTime = new Date().getTime();

	@JsonIgnore
	private Boolean expired;

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public int getExpiresIn() {
		return expiresIn;
	}

	public void setExpiresIn(int expiresIn) {
		this.expiresIn = expiresIn;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public long getCreateTime() {
		return createTime;
	}

	public void setCreateTime(long createTime) {
		this.createTime = createTime;
	}

	public Boolean getExpired() {
		return expired;
	}

	public void setExpired(Boolean expired) {
		this.expired = expired;
	}

	@JsonIgnore
	public boolean isExpired() {
		if (expired != null)
			return expired;
		if (expiresIn <= 0)
			return false;
		return (new Date().getTime() - createTime) / 1000 >= expiresIn;
	}

	@Override
	public String toString() {
		return "OAuthToken [accessToken=" + accessToken + ", expiresIn=" + expiresIn + ", refreshToken=" + refreshToken
				+ ", createTime=" + createTime + "]";
	}

}