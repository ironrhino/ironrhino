package org.ironrhino.security.oauth.server.service;

import java.util.Optional;

public interface AuthorizationCodeResolver {

	default boolean accepts(String code) {
		return true;
	}

	Optional<String> resolver(String code);

}
