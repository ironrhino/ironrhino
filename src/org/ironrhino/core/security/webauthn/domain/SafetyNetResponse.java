package org.ironrhino.core.security.webauthn.domain;

import lombok.Data;

@Data
public class SafetyNetResponse {

	private long timestampMs;

	private String nonce;

	private String apkPackageName;

	private byte[] apkCertificateDigestSha256;

	private boolean ctsProfileMatch;

	private boolean basicIntegrity;

}
