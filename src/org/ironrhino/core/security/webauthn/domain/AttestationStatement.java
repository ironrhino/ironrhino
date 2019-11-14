package org.ironrhino.core.security.webauthn.domain;

import java.util.List;

import org.ironrhino.core.security.webauthn.domain.cose.Algorithm;

import lombok.Value;

@Value
public class AttestationStatement {

	private byte[] sig;

	private List<byte[]> x5c;

	// for packed

	private Algorithm alg;

	private byte[] ecdaaKeyId;

	// for tpm
	private String ver;

	private byte[] certInfo;

	private byte[] pubArea;

	// for android-safetynet
	private byte[] response;

}
