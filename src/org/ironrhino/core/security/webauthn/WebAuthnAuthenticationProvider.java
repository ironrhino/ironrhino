package org.ironrhino.core.security.webauthn;

import org.ironrhino.core.security.webauthn.domain.AuthenticatorAssertionResponse;
import org.ironrhino.core.security.webauthn.domain.PublicKeyCredential;
import org.ironrhino.core.security.webauthn.internal.Utils;
import org.ironrhino.core.spring.configuration.ApplicationContextPropertiesConditional;
import org.ironrhino.core.spring.security.DefaultDaoAuthenticationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.NullAuthoritiesMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

@ApplicationContextPropertiesConditional(key = "webAuthn.enabled", value = "true")
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class WebAuthnAuthenticationProvider implements AuthenticationProvider {

	@Autowired
	private WebAuthnService webAuthnService;

	@Autowired
	private UserDetailsService userDetailsService;

	@Autowired
	private DefaultDaoAuthenticationProvider daoAuthenticationProvider;

	@Autowired(required = false)
	private GrantedAuthoritiesMapper authoritiesMapper = new NullAuthoritiesMapper();

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		if (!supports(authentication.getClass()))
			return null;
		UsernamePasswordAuthenticationToken upat = (UsernamePasswordAuthenticationToken) authentication;
		String json = String.valueOf(upat.getCredentials());
		if (!json.startsWith("{"))
			return null;
		try {
			PublicKeyCredential<AuthenticatorAssertionResponse> credential = Utils.JSON_OBJECTMAPPER.readValue(json,
					new TypeReference<PublicKeyCredential<AuthenticatorAssertionResponse>>() {
					});
			if (credential.getId() == null)
				return null;
			UserDetails user = userDetailsService.loadUserByUsername(authentication.getName());
			daoAuthenticationProvider.getPreAuthenticationChecks().check(user);
			webAuthnService.verifyAssertion(credential, user.getUsername());
			daoAuthenticationProvider.getPostAuthenticationChecks().check(user);

			return new UsernamePasswordAuthenticationToken(user, credential,
					authoritiesMapper.mapAuthorities(user.getAuthorities()));
		} catch (JsonProcessingException e) {
			return null;
		}
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return UsernamePasswordAuthenticationToken.class.isAssignableFrom(clazz);
	}

}
