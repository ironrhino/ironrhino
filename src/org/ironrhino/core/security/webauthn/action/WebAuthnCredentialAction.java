package org.ironrhino.core.security.webauthn.action;

import java.time.LocalDateTime;

import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.JsonConfig;
import org.ironrhino.core.security.role.UserRole;
import org.ironrhino.core.security.webauthn.StoredCredentialService;
import org.ironrhino.core.security.webauthn.WebAuthnEnabled;
import org.ironrhino.core.security.webauthn.WebAuthnService;
import org.ironrhino.core.security.webauthn.domain.AuthenticatorAttestationResponse;
import org.ironrhino.core.security.webauthn.domain.PublicKeyCredential;
import org.ironrhino.core.security.webauthn.domain.PublicKeyCredentialCreationOptions;
import org.ironrhino.core.security.webauthn.internal.Utils;
import org.ironrhino.core.security.webauthn.model.WebAuthnCredential;
import org.ironrhino.core.struts.EntityAction;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.NotReadablePropertyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import com.fasterxml.jackson.core.type.TypeReference;
import com.opensymphony.xwork2.interceptor.annotations.InputConfig;

import lombok.Getter;
import lombok.Setter;

@WebAuthnEnabled
@AutoConfig
@Authorize(ifAnyGranted = UserRole.ROLE_ADMINISTRATOR)
public class WebAuthnCredentialAction extends EntityAction<WebAuthnCredential> {

	private static final long serialVersionUID = -5531177698516519716L;

	@Autowired
	protected UserDetailsService userDetailsService;

	@Autowired
	protected WebAuthnService webAuthnService;

	@Autowired
	protected StoredCredentialService storedCredentialService;

	@Getter
	private PublicKeyCredentialCreationOptions options;

	@Getter
	@Setter
	private String username;

	@Getter
	@Setter
	private LocalDateTime expiryTime;

	@Getter
	@Setter
	private String credential;

	@InputConfig(resultName = "input")
	public String create() throws Exception {
		PublicKeyCredential<AuthenticatorAttestationResponse> cred = Utils.JSON_OBJECTMAPPER.readValue(credential,
				new TypeReference<PublicKeyCredential<AuthenticatorAttestationResponse>>() {
				});
		webAuthnService.verifyAttestation(cred, username);
		if (expiryTime != null)
			storedCredentialService.updateExpiryTime(cred.getId(), expiryTime);
		notify("operate.success");
		return SUCCESS;
	}

	@JsonConfig(root = "options")
	public String options() {
		UserDetails user = userDetailsService.loadUserByUsername(username);
		BeanWrapperImpl bw = new BeanWrapperImpl(user);
		String id;
		try {
			id = String.valueOf(bw.getPropertyValue("id"));
		} catch (NotReadablePropertyException e) {
			id = user.getUsername();
		}
		String name = user.toString();
		options = webAuthnService.buildCreationOptions(id, user.getUsername(), name);
		return JSON;
	}

}
