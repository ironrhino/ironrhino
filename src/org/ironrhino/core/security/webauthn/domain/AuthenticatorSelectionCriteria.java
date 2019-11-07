package org.ironrhino.core.security.webauthn.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticatorSelectionCriteria {

	private AuthenticatorAttachment authenticatorAttachment;

	private boolean requireResidentKey = false;

	private UserVerificationRequirement userVerification = UserVerificationRequirement.preferred;

}