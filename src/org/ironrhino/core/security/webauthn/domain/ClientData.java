package org.ironrhino.core.security.webauthn.domain;

import java.io.IOException;
import java.net.URI;

import org.ironrhino.core.security.webauthn.internal.Utils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

@Data
public class ClientData {

	private final PublicKeyCredentialOperationType type;

	private final String challenge;

	private final String origin;

	private final TokenBinding tokenBinding;

	@JsonIgnore
	@Setter(AccessLevel.PRIVATE)
	private byte[] rawData;

	@JsonCreator
	public static ClientData valueOf(String input) throws IOException {
		byte[] rawData = Utils.decodeBase64url(input);
		ClientData cd = Utils.JSON_OBJECTMAPPER.readValue(rawData, ClientData.class);
		cd.rawData = rawData;
		return cd;
	}

	public void verify(PublicKeyCredentialOperationType type, String rpId, String challenge) throws Exception {
		if (this.type != type)
			throw new IllegalArgumentException("Invalid type: " + type);
		if (!this.challenge.equals(challenge))
			throw new IllegalArgumentException("Challenge failed");
		if (!URI.create(origin).getHost().equals(rpId))
			throw new IllegalArgumentException("Mismatched origin");
		if (tokenBinding != null && tokenBinding.getStatus() == TokenBindingStatus.present) {
			if (tokenBinding.getId() == null)
				throw new IllegalArgumentException("Missing id for TokenBinding");
			// TODO verify token bing id
		}
	}

}
