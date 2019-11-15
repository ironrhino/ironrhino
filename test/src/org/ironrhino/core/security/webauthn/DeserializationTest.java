package org.ironrhino.core.security.webauthn;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Base64;

import org.ironrhino.core.security.webauthn.domain.Attestation;
import org.ironrhino.core.security.webauthn.domain.AttestationStatement;
import org.ironrhino.core.security.webauthn.domain.AttestationStatementFormat;
import org.ironrhino.core.security.webauthn.domain.AttestedCredential;
import org.ironrhino.core.security.webauthn.domain.AuthenticatorAssertionResponse;
import org.ironrhino.core.security.webauthn.domain.AuthenticatorAttestationResponse;
import org.ironrhino.core.security.webauthn.domain.AuthenticatorData;
import org.ironrhino.core.security.webauthn.domain.ClientData;
import org.ironrhino.core.security.webauthn.domain.PublicKeyCredential;
import org.ironrhino.core.security.webauthn.domain.PublicKeyCredentialOperationType;
import org.ironrhino.core.security.webauthn.domain.cose.Algorithm;
import org.ironrhino.core.security.webauthn.domain.cose.EC2Key;
import org.ironrhino.core.security.webauthn.domain.cose.Curve;
import org.ironrhino.core.security.webauthn.domain.cose.Key;
import org.ironrhino.core.security.webauthn.domain.cose.KeyType;
import org.ironrhino.core.security.webauthn.internal.Utils;
import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DeserializationTest {

	private static final String KEY_JSON = "{\"3\":-7,\"-1\":1,\"-2\":\"KAdACr2BYzM22clwTv3d/jS4SqV3BEwSu6SSkcu9kqQ=\",\"-3\":\"zBh9ZybYDt8uzI1KA5juXxQ4Rn5gB0DAmDtg//e2F2s=\",\"1\":2}";
	public static final String ATTESTATION_JSON = "{\"id\":\"VwFB9K0r2NntrHoFSnscGh7wX2gAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA==\",\"type\":\"public-key\",\"response\":{\"attestationObject\":\"o2NmbXRoZmlkby11MmZnYXR0U3RtdKJjc2lnWEYwRAIgGVDLDEKzWA+oRRvQ0Hn610ROOO5WMvblcDVsDyPp7M0CIBWJ1pXufZU+nfzVTLP5dPU7IyuowC2wf36T4bSGHHETY3g1Y4FZAYIwggF+MIIBJKADAgECAgEBMAoGCCqGSM49BAMCMDwxETAPBgNVBAMMCFNvZnQgVTJGMRQwEgYDVQQKDAtHaXRIdWIgSW5jLjERMA8GA1UECwwIU2VjdXJpdHkwHhcNMTcwNzI2MjAwOTA4WhcNMjcwNzI0MjAwOTA4WjA8MREwDwYDVQQDDAhTb2Z0IFUyRjEUMBIGA1UECgwLR2l0SHViIEluYy4xETAPBgNVBAsMCFNlY3VyaXR5MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE9pyrJBRLtO+H9w8jHFzU9XgErPjgxrKz41IYPYA5H2vSedJqTINkdObC2iOT/6wdUDRsXCOQZVeTPsuT/27e0aMXMBUwEwYLKwYBBAGC5RwCAQEEBAMCAwgwCgYIKoZIzj0EAwIDSAAwRQIhAP4iHZe46uoSu59CFIUPSBdlteCVk16ho9ZtD7FvOfciAiBk19wvXGw4Kvdl9XhqObCxSpdFKO993yECFRuIStRBemhhdXRoRGF0YVjKSZYN5YgOjGh0NBcPZHZgW4/krrmihjLHmVzzuoMdl2NBAAAAAAAAAAAAAAAAAAAAAAAAAAAARlcBQfStK9jZ7ax6BUp7HBoe8F9oAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAClAQIDJiABIVggP28u5Kd+UD7B1nGFqih2Vs2oGgYumGPhlUEmZK88kxsiWCBXyPuTvQ3fdajIrynWKMIsd8FxeNJ5EdHP4xb9+MCm/A==\",\"clientDataJSON\":\"eyJjaGFsbGVuZ2UiOiJja0ozYW5aUGJIWkRlbTQzVEVJME9FaE1PVEZFYVhab2FHTlBTV0ZtVjI4IiwiZXh0cmFfa2V5c19tYXlfYmVfYWRkZWRfaGVyZSI6ImRvIG5vdCBjb21wYXJlIGNsaWVudERhdGFKU09OIGFnYWluc3QgYSB0ZW1wbGF0ZS4gU2VlIGh0dHBzOi8vZ29vLmdsL3lhYlBleCIsIm9yaWdpbiI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODA4MCIsInR5cGUiOiJ3ZWJhdXRobi5jcmVhdGUifQ==\"}}";
	public static final String ASSERTION_JSON = "{\"id\":\"VwFB9K0r2NntrHoFSnscGh7wX2gAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA==\",\"response\":{\"authenticatorData\":\"SZYN5YgOjGh0NBcPZHZgW4/krrmihjLHmVzzuoMdl2MBAAAAbA==\",\"clientDataJSON\":\"eyJjaGFsbGVuZ2UiOiJORWd3Vlc1S2IzTmxaR0psWnpGblJFTldlRzFITkVWb1VXdEZjRFZGUlRZIiwiZXh0cmFfa2V5c19tYXlfYmVfYWRkZWRfaGVyZSI6ImRvIG5vdCBjb21wYXJlIGNsaWVudERhdGFKU09OIGFnYWluc3QgYSB0ZW1wbGF0ZS4gU2VlIGh0dHBzOi8vZ29vLmdsL3lhYlBleCIsIm9yaWdpbiI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODA4MCIsInR5cGUiOiJ3ZWJhdXRobi5nZXQifQ==\",\"signature\":\"MEUCIHxldwt/+ELzkLR5D/rBwP0tk0zZJK6h5PmlElXyODkyAiEAhP3j4r+3Y5XjbrPzFUW/Oj75uolsOxkFg21E3a5JADc=\"}}";

	@Test
	public void testKey() throws Exception {
		EC2Key key = (EC2Key) new ObjectMapper().readValue(KEY_JSON, Key.class);
		assertThat(key.getKeyType(), equalTo(KeyType.EC2));
		assertThat(key.getAlgorithm(), equalTo(Algorithm.ES256));
		assertThat(key.getCurve(), equalTo(Curve.secp256r1));
		assertThat(key.getX(), equalTo(Base64.getDecoder().decode("KAdACr2BYzM22clwTv3d/jS4SqV3BEwSu6SSkcu9kqQ=")));
		assertThat(key.getY(), equalTo(Base64.getDecoder().decode("zBh9ZybYDt8uzI1KA5juXxQ4Rn5gB0DAmDtg//e2F2s=")));
	}

	@Test
	public void testAuthenticatorAttestationResponse() throws Exception {
		PublicKeyCredential<AuthenticatorAttestationResponse> credential = Utils.JSON_OBJECTMAPPER.readValue(
				ATTESTATION_JSON, new TypeReference<PublicKeyCredential<AuthenticatorAttestationResponse>>() {
				});
		AuthenticatorAttestationResponse response = credential.getResponse();
		ClientData cd = response.getClientData();
		assertThat(cd.getType(), equalTo(PublicKeyCredentialOperationType.CREATE));
		assertThat(cd.getChallenge(), equalTo("ckJ3anZPbHZDem43TEI0OEhMOTFEaXZoaGNPSWFmV28"));
		assertThat(cd.getOrigin(), equalTo("http://localhost:8080"));

		Attestation attestation = response.getAttestationObject();
		assertThat(attestation.getFmt(), equalTo(AttestationStatementFormat.fido_u2f));

		AuthenticatorData authData = attestation.getAuthData();
		assertThat(authData.getSignCount(), equalTo(0));
		assertThat(authData.isUserPresent(), equalTo(true));
		assertThat(authData.isUserVerified(), equalTo(false));
		assertThat(authData.hasAttestedcredentialData(), equalTo(true));
		assertThat(authData.hasExtensionData(), equalTo(false));
		assertThat(authData.getRpIdHash(),
				equalTo(Utils.decodeBase64url("SZYN5YgOjGh0NBcPZHZgW4_krrmihjLHmVzzuoMdl2M")));

		AttestedCredential attestedCredential = authData.getAttestedCredential();
		assertThat(attestedCredential.getCredentialId(), equalTo(credential.getId()));
		assertThat(attestedCredential.getCredentialId(), equalTo(Utils.decodeBase64url(
				"VwFB9K0r2NntrHoFSnscGh7wX2gAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")));
		assertThat(attestedCredential.getAaguid(), equalTo(Base64.getDecoder().decode("AAAAAAAAAAAAAAAAAAAAAA")));
		assertThat(attestedCredential.getCredentialPublicKey(), instanceOf(EC2Key.class));

		EC2Key key = (EC2Key) attestedCredential.getCredentialPublicKey();
		assertThat(key.getKeyType(), equalTo(KeyType.EC2));
		assertThat(key.getAlgorithm(), equalTo(Algorithm.ES256));
		assertThat(key.getCurve(), equalTo(Curve.secp256r1));
		assertThat(key.getX(), equalTo(Base64.getDecoder().decode("P28u5Kd+UD7B1nGFqih2Vs2oGgYumGPhlUEmZK88kxs=")));
		assertThat(key.getY(), equalTo(Base64.getDecoder().decode("V8j7k70N33WoyK8p1ijCLHfBcXjSeRHRz+MW/fjApvw=")));

		AttestationStatement attStmt = attestation.getAttStmt();
		assertThat(attStmt.getSig(), equalTo(Base64.getDecoder().decode(
				"MEQCIBlQywxCs1gPqEUb0NB5+tdETjjuVjL25XA1bA8j6ezNAiAVidaV7n2VPp381Uyz+XT1OyMrqMAtsH9+k+G0hhxxEw==")));
		assertThat(attStmt.getX5c().get(0), equalTo(Base64.getDecoder().decode(
				"MIIBfjCCASSgAwIBAgIBATAKBggqhkjOPQQDAjA8MREwDwYDVQQDDAhTb2Z0IFUyRjEUMBIGA1UECgwLR2l0SHViIEluYy4xETAPBgNVBAsMCFNlY3VyaXR5MB4XDTE3MDcyNjIwMDkwOFoXDTI3MDcyNDIwMDkwOFowPDERMA8GA1UEAwwIU29mdCBVMkYxFDASBgNVBAoMC0dpdEh1YiBJbmMuMREwDwYDVQQLDAhTZWN1cml0eTBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABPacqyQUS7Tvh/cPIxxc1PV4BKz44Mays+NSGD2AOR9r0nnSakyDZHTmwtojk/+sHVA0bFwjkGVXkz7Lk/9u3tGjFzAVMBMGCysGAQQBguUcAgEBBAQDAgMIMAoGCCqGSM49BAMCA0gAMEUCIQD+Ih2XuOrqErufQhSFD0gXZbXglZNeoaPWbQ+xbzn3IgIgZNfcL1xsOCr3ZfV4ajmwsUqXRSjvfd8hAhUbiErUQXo=")));

	}

	@Test
	public void testAuthenticatorAssertionResponse() throws Exception {
		PublicKeyCredential<AuthenticatorAssertionResponse> credential = Utils.JSON_OBJECTMAPPER
				.readValue(ASSERTION_JSON, new TypeReference<PublicKeyCredential<AuthenticatorAssertionResponse>>() {
				});
		AuthenticatorAssertionResponse response = credential.getResponse();
		ClientData cd = response.getClientData();
		assertThat(cd.getType(), equalTo(PublicKeyCredentialOperationType.GET));
		assertThat(cd.getChallenge(), equalTo("NEgwVW5Kb3NlZGJlZzFnRENWeG1HNEVoUWtFcDVFRTY"));
		assertThat(cd.getOrigin(), equalTo("http://localhost:8080"));

		AuthenticatorData authData = response.getAuthenticatorData();

		assertThat(authData.getSignCount(), equalTo(108));
		assertThat(authData.isUserPresent(), equalTo(true));
		assertThat(authData.isUserVerified(), equalTo(false));
		assertThat(authData.hasAttestedcredentialData(), equalTo(false));
		assertThat(authData.hasExtensionData(), equalTo(false));
		assertThat(authData.getRpIdHash(),
				equalTo(Utils.decodeBase64url("SZYN5YgOjGh0NBcPZHZgW4_krrmihjLHmVzzuoMdl2M")));

		assertThat(response.getSignature(), equalTo(Base64.getDecoder().decode(
				"MEUCIHxldwt/+ELzkLR5D/rBwP0tk0zZJK6h5PmlElXyODkyAiEAhP3j4r+3Y5XjbrPzFUW/Oj75uolsOxkFg21E3a5JADc=")));
	}

}
