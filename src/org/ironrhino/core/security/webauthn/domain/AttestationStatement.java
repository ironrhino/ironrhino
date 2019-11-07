package org.ironrhino.core.security.webauthn.domain;

import java.util.List;

import org.ironrhino.core.security.webauthn.domain.cose.Algorithm;

import lombok.Data;

@Data
public class AttestationStatement {

	private Algorithm alg;

	private byte[] sig;

	private List<byte[]> x5c;

	private byte[] attestnCert;

	private byte[] ecdaaKeyId;

	// for tpm
	private String ver;

	private byte[] certInfo;

	private byte[] pubArea;

	// for android-safetynet
	private byte[] response;

}
