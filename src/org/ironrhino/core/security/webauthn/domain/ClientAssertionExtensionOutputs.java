package org.ironrhino.core.security.webauthn.domain;

import java.util.HashSet;
import java.util.Set;

import lombok.Data;

@Data
public class ClientAssertionExtensionOutputs implements ClientExtensionOutputs {

	private Boolean appid;

	@Override
	public Set<String> getExtensionIds() {
		Set<String> ids = new HashSet<>();
		if (appid != null)
			ids.add("appid");
		return ids;
	}

}