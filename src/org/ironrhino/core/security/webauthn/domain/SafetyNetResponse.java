package org.ironrhino.core.security.webauthn.domain;

import java.util.List;

import lombok.Value;

@Value
public class SafetyNetResponse {

	private long timestampMs;

	private String nonce;

	private String apkPackageName;

	private byte[] apkDigestSha256;

	private List<byte[]> apkCertificateDigestSha256;

	private boolean ctsProfileMatch;

	private boolean basicIntegrity;

}
