package org.ironrhino.core.security.webauthn.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
// https://www.w3.org/TR/webauthn/#dictdef-publickeycredentialrequestoptions
public class PublicKeyCredentialRequestOptions {

	private String challenge;

	private Long timeout;

	private String rpId;

	private List<PublicKeyCredentialDescriptor> allowCredentials = new ArrayList<>();

	private UserVerificationRequirement userVerification = UserVerificationRequirement.preferred;

	private Map<String, Object> extensions;

}
