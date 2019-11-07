package org.ironrhino.core.security.webauthn.domain;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.ironrhino.core.security.webauthn.internal.Utils;
import org.ironrhino.core.util.CodecUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;

import lombok.Data;

@Data
public class AuthenticatorData {

	private final byte[] rpIdHash;

	private final Map<String, Boolean> flags;

	private final int signCount;

	private final AttestedCredentialData attestedCredentialData;

	private final Map<String, String> extensions;

	@JsonIgnore
	private final byte[] rawData;

	@JsonCreator
	public AuthenticatorData(byte[] input) throws Exception {
		this.rawData = input;

		rpIdHash = new byte[32];
		System.arraycopy(input, 0, rpIdHash, 0, rpIdHash.length);

		byte b = input[32];
		flags = new HashMap<>();
		flags.put("UP", ((b >> 0) & 0x1) == 1);
		flags.put("UV", ((b >> 2) & 0x1) == 1);
		flags.put("AT", ((b >> 6) & 0x1) == 1);
		flags.put("ED", ((b >> 7) & 0x1) == 1);

		byte[] count = new byte[4];
		System.arraycopy(input, 33, count, 0, count.length);
		signCount = (int) (((count[0] & 0xFF) << 24) | ((count[1] & 0xFF) << 16) | ((count[2] & 0xFF) << 8)
				| (count[3] & 0xFF));

		AttestedCredentialData attestedCredentialData = null;
		Map<String, String> extensions = null;
		if (input.length > 37) {
			byte[] data = new byte[input.length - 37];
			System.arraycopy(input, 37, data, 0, data.length);
			if (flags.get("AT")) {
				attestedCredentialData = new AttestedCredentialData(data);
			} else if (flags.get("ED")) {
				extensions = Utils.CBOR_OBJECTMAPPER.readValue(data, new TypeReference<Map<String, String>>() {
				});
			}
		}
		this.attestedCredentialData = attestedCredentialData;
		this.extensions = extensions;
	}

	public void verify(String rpId, UserVerificationRequirement userVerification) {
		if (!Arrays.equals(rpIdHash, CodecUtils.sha256(rpId)))
			throw new RuntimeException("Mismatched rpIdHash");
		Boolean up = flags.get("UP");
		if (up == null || !up)
			throw new RuntimeException("User not present");
		Boolean uv = flags.get("UV");
		if (userVerification == UserVerificationRequirement.required && (uv == null || !uv))
			throw new RuntimeException("User verification required");
	}

}
