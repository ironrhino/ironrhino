package org.ironrhino.core.security.webauthn.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
// https://www.w3.org/TR/webauthn/#dictdef-publickeycredentialcreationoptions
public class PublicKeyCredentialCreationOptions {

	private PublicKeyCredentialRpEntity rp;

	private PublicKeyCredentialUserEntity user;

	private String challenge;

	private List<PublicKeyCredentialParameters> pubKeyCredParams;

	private Long timeout;

	private List<PublicKeyCredentialDescriptor> excludeCredentials = new ArrayList<>();

	private AuthenticatorSelectionCriteria authenticatorSelection;

	private AttestationConveyancePreference attestation = AttestationConveyancePreference.none;

	private Map<String, Object> extensions;

}
