package org.ironrhino.security.oauth.client.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class OAuthToken implements java.io.Serializable {

	private static final long serialVersionUID = 51906769556727320L;

	@Getter
	protected String source;

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
