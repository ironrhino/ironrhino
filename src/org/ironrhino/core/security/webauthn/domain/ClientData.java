package org.ironrhino.core.security.webauthn.domain;

import static org.ironrhino.core.security.webauthn.internal.Utils.JSON_OBJECTMAPPER;

import java.io.IOException;
import java.net.URL;

import org.ironrhino.core.security.webauthn.internal.Utils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

@Data
public class ClientData {

	private PublicKeyCredentialOperationType type;

	private String challenge;

	private String origin;

	private TokenBinding tokenBinding;

	@JsonIgnore
	private byte[] rawData;

	@JsonCreator
	public static ClientData valueOf(String input) throws IOException {
		byte[] rawData = Utils.decodeBase64url(input);
		ClientData cd = JSON_OBJECTMAPPER.readValue(rawData, ClientData.class);
		cd.rawData = rawData;
		return cd;
	}

	public void verify(PublicKeyCredentialOperationType type, String rpId, String challenge) throws Exception {
		if (this.type != type)
			throw new IllegalArgumentException("Invalid type: " + type);
		if (!this.challenge.equals(challenge))
			throw new IllegalArgumentException("Challenge failed");
		URL url = new URL(origin);
		if (!url.getHost().equals(rpId))
			throw new IllegalArgumentException("Mismatched origin");
		if (tokenBinding != null && tokenBinding.getStatus() == TokenBindingStatus.present) {
			if (tokenBinding.getId() == null)
				throw new IllegalArgumentException("Missing id for TokenBinding");
			// TODO verify token bing id
		}
	}

}
