package org.ironrhino.core.security.webauthn.action;

import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.JsonConfig;
import org.ironrhino.core.security.webauthn.WebAuthnService;
import org.ironrhino.core.security.webauthn.domain.AuthenticatorAssertionResponse;
import org.ironrhino.core.security.webauthn.domain.PublicKeyCredential;
import org.ironrhino.core.security.webauthn.domain.PublicKeyCredentialRequestOptions;
import org.ironrhino.core.struts.BaseAction;
import org.ironrhino.core.util.AuthzUtils;
import org.ironrhino.core.util.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.type.TypeReference;

import lombok.Getter;

@AutoConfig
public class AuthenticateAction extends BaseAction {

	private static final long serialVersionUID = -5531177698516519716L;

	@Autowired
	protected WebAuthnService webAuthnService;

	@Getter
	private PublicKeyCredentialRequestOptions options;

	public String execute() throws Exception {
		PublicKeyCredential<AuthenticatorAssertionResponse> credential = JsonUtils.fromJson(requestBody,
				new TypeReference<PublicKeyCredential<AuthenticatorAssertionResponse>>() {
				});

		System.out.println(JsonUtils.toJson(credential));
		webAuthnService.verifyAssertion(credential, AuthzUtils.getUsername());
		return NONE;
	}

	@JsonConfig(root = "options")
	public String options() {
		options = webAuthnService.buildRequestOptions(AuthzUtils.getUsername());
		return JSON;
	}

}
