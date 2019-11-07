package org.ironrhino.core.security.webauthn.domain;

import java.util.Collections;
import java.util.Set;

import lombok.Data;

@Data
public class ClientAttestationExtensionOutputs implements ClientExtensionOutputs {

	@Override
	public Set<String> getExtensionIds() {
		return Collections.emptySet();
	}

}