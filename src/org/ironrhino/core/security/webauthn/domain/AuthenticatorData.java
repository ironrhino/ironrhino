package org.ironrhino.core.security.webauthn.domain;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.ironrhino.core.security.webauthn.internal.Utils;
import org.ironrhino.core.util.CodecUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;

import lombok.Value;

@Value
public class AuthenticatorData {

	private byte[] rpIdHash;

	private Map<String, Boolean> flags;

	private int signCount;

	private AttestedCredential attestedCredential;

	private Map<String, Object> extensions;

	@JsonIgnore
	private byte[] rawData;

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
		signCount = ((count[0] & 0xFF) << 24) | ((count[1] & 0xFF) << 16) | ((count[2] & 0xFF) << 8)
				| (count[3] & 0xFF);

		AttestedCredential attestedCredential = null;
		Map<String, Object> extensions = null;
		if (input.length > 37) {
			byte[] data = new byte[input.length - 37];
			System.arraycopy(input, 37, data, 0, data.length);
			if (hasAttestedcredentialData()) {
				attestedCredential = AttestedCredential.valueOf(data);
			} else if (hasExtensionData()) {
				extensions = Utils.CBOR_OBJECTMAPPER.readValue(data, new TypeReference<Map<String, Object>>() {
				});
			}
		}
		this.attestedCredential = attestedCredential;
		this.extensions = extensions;
	}

	@JsonIgnore
	public boolean isUserPresent() {
		Boolean b = flags.get("UP");
		return b != null && b;
	}

	@JsonIgnore
	public boolean isUserVerified() {
		Boolean b = flags.get("UV");
		return b != null && b;
	}

	public boolean hasAttestedcredentialData() {
		Boolean b = flags.get("AT");
		return b != null && b;
	}

	public boolean hasExtensionData() {
		Boolean b = flags.get("ED");
		return b != null && b;
	}

	public void verify(String rpId, UserVerificationRequirement userVerification) {
		if (!Arrays.equals(rpIdHash, CodecUtils.sha256(rpId)))
			throw new IllegalArgumentException("Mismatched rpIdHash");
		if (!isUserPresent())
			throw new IllegalArgumentException("User not present");
		if (userVerification == UserVerificationRequirement.required && !isUserVerified())
			throw new IllegalArgumentException("User verification required");
	}

}
