package org.ironrhino.security.oauth.client.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class OAuthToken implements java.io.Serializable {

	private static final long serialVersionUID = 51906769556727320L;

	protected String source;

	public OAuthToken() {
	}

	public OAuthToken(String source) {
		setSource(source);
	}

	@JsonIgnore
	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		if (source != null)
			source = source.trim();
		this.source = source;
	}

	@Override
	public String toString() {
		return source;
	}

}
