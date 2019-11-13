package org.ironrhino.core.security.webauthn.action;

import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.JsonConfig;
import org.ironrhino.core.security.webauthn.WebAuthnService;
import org.ironrhino.core.security.webauthn.domain.AuthenticatorAssertionResponse;
import org.ironrhino.core.security.webauthn.domain.AuthenticatorAttestationResponse;
import org.ironrhino.core.security.webauthn.domain.PublicKeyCredential;
import org.ironrhino.core.security.webauthn.domain.PublicKeyCredentialCreationOptions;
import org.ironrhino.core.security.webauthn.domain.PublicKeyCredentialRequestOptions;
import org.ironrhino.core.struts.BaseAction;
import org.ironrhino.core.util.AuthzUtils;
import org.ironrhino.core.util.JsonUtils;
import org.ironrhino.security.model.User;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.type.TypeReference;

import lombok.Getter;

@AutoConfig
public class WebAuthnAction extends BaseAction {

	private static final long serialVersionUID = -5531177698516519716L;

	@Autowired
	protected WebAuthnService webAuthnService;

	@Getter
	private PublicKeyCredentialCreationOptions creationOptions;

	@Getter
	private PublicKeyCredentialRequestOptions requestOptions;

	@JsonConfig(root = "creationOptions")
	public String creationOptions() {
		User user = AuthzUtils.getUserDetails(User.class);
		creationOptions = webAuthnService.buildCreationOptions(user.getId(), user.getUsername(), user.getName());
		return JSON;
	}

	public String register() throws Exception {
		PublicKeyCredential<AuthenticatorAttestationResponse> credential = JsonUtils.fromJson(requestBody,
				new TypeReference<PublicKeyCredential<AuthenticatorAttestationResponse>>() {
				});

		System.out.println(JsonUtils.toJson(credential));
		webAuthnService.verifyAttestation(credential, AuthzUtils.getUsername());
		return NONE;
	}

	@JsonConfig(root = "requestOptions")
	public String requestOptions() {
		requestOptions = webAuthnService.buildRequestOptions(AuthzUtils.getUsername());
		return JSON;
	}

	public String authenticate() throws Exception {
		PublicKeyCredential<AuthenticatorAssertionResponse> credential = JsonUtils.fromJson(requestBody,
				new TypeReference<PublicKeyCredential<AuthenticatorAssertionResponse>>() {
				});

		System.out.println(JsonUtils.toJson(credential));
		webAuthnService.verifyAssertion(credential, AuthzUtils.getUsername());
		return NONE;
	}

}
