package org.ironrhino.rest.client.token;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface Token extends Serializable {

	public String getAccessToken();

	public String getRefreshToken();

	public String getTokenType();

	public int getExpiresIn();

	public long getCreateTime();

	@JsonIgnore
	public default boolean isExpired() {
		int expiresIn = getExpiresIn();
		long createTime = getCreateTime();
		if (expiresIn <= 0 || createTime <= 0)
			return false;
		int offset = expiresIn > 3600 ? expiresIn / 20 : 300;
		return (System.currentTimeMillis() - createTime) / 1000 > (expiresIn - offset);
	}

}
