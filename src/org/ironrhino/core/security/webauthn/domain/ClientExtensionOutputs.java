package org.ironrhino.core.security.webauthn.domain;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface ClientExtensionOutputs {

	@JsonIgnore
	Set<String> getExtensionIds();

}