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
	private static final String ATTESTATION_JSON = "{\"id\":\"8YcOfl/M8dYmDpssEotuF+fOHVUAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA==\",\"type\":\"public-key\",\"response\":{\"attestationObject\":\"o2NmbXRoZmlkby11MmZnYXR0U3RtdKJjc2lnWEgwRgIhAMHWwOVg6mYMRdAQlSy4nTRUClD104ig7EwFJUEocgrCAiEAlI1vI49ivjqyBlV9HymdqDw+MYiy5gjdElPa5svSzqxjeDVjgVkBgjCCAX4wggEkoAMCAQICAQEwCgYIKoZIzj0EAwIwPDERMA8GA1UEAwwIU29mdCBVMkYxFDASBgNVBAoMC0dpdEh1YiBJbmMuMREwDwYDVQQLDAhTZWN1cml0eTAeFw0xNzA3MjYyMDA5MDhaFw0yNzA3MjQyMDA5MDhaMDwxETAPBgNVBAMMCFNvZnQgVTJGMRQwEgYDVQQKDAtHaXRIdWIgSW5jLjERMA8GA1UECwwIU2VjdXJpdHkwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAAT2nKskFEu074f3DyMcXNT1eASs+ODGsrPjUhg9gDkfa9J50mpMg2R05sLaI5P/rB1QNGxcI5BlV5M+y5P/bt7RoxcwFTATBgsrBgEEAYLlHAIBAQQEAwIDCDAKBggqhkjOPQQDAgNIADBFAiEA/iIdl7jq6hK7n0IUhQ9IF2W14JWTXqGj1m0PsW859yICIGTX3C9cbDgq92X1eGo5sLFKl0Uo733fIQIVG4hK1EF6aGF1dGhEYXRhWMpJlg3liA6MaHQ0Fw9kdmBbj+SuuaKGMseZXPO6gx2XY0EAAAAAAAAAAAAAAAAAAAAAAAAAAABG8YcOfl/M8dYmDpssEotuF+fOHVUAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAKUBAgMmIAEhWCAoB0AKvYFjMzbZyXBO/d3+NLhKpXcETBK7pJKRy72SpCJYIMwYfWcm2A7fLsyNSgOY7l8UOEZ+YAdAwJg7YP/3thdr\",\"clientDataJSON\":\"eyJjaGFsbGVuZ2UiOiJlVWR5ZGpsMWFGUkRaVzl0VlV4cGVHNHlSWE5zT1dGMGFGUjRlRzFXYWpVIiwiZXh0cmFfa2V5c19tYXlfYmVfYWRkZWRfaGVyZSI6ImRvIG5vdCBjb21wYXJlIGNsaWVudERhdGFKU09OIGFnYWluc3QgYSB0ZW1wbGF0ZS4gU2VlIGh0dHBzOi8vZ29vLmdsL3lhYlBleCIsIm9yaWdpbiI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODA4MCIsInR5cGUiOiJ3ZWJhdXRobi5jcmVhdGUifQ==\"}}";
	private static final String ASSERTION_JSON = "{\"id\":\"6s1/7mPR0C6ruk0uszQPPbFG74AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA==\",\"response\":{\"authenticatorData\":\"SZYN5YgOjGh0NBcPZHZgW4/krrmihjLHmVzzuoMdl2MBAAAAaA==\",\"clientDataJSON\":\"eyJjaGFsbGVuZ2UiOiJUMHRPTlRaNFpGVnlUazVNTnpBMU9YTjBRVEowTW5sdmNEUTFNRmRUVERJIiwib3JpZ2luIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwIiwidHlwZSI6IndlYmF1dGhuLmdldCJ9\",\"signature\":\"MEQCIBVF6MRCjeMhXF5m+PhbtPUFt5LgPg+w465LsTEeQ9dFAiAbOiZA8gbHKH4pVbdat0tVulx50CZW2XY5+EziXJvSdQ==\"}}";

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
		assertThat(cd.getChallenge(), equalTo("eUdydjl1aFRDZW9tVUxpeG4yRXNsOWF0aFR4eG1WajU"));
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
				"8YcOfl_M8dYmDpssEotuF-fOHVUAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")));
		assertThat(attestedCredential.getAaguid(), equalTo(Base64.getDecoder().decode("AAAAAAAAAAAAAAAAAAAAAA")));
		assertThat(attestedCredential.getCredentialPublicKey(), instanceOf(EC2Key.class));

		EC2Key key = (EC2Key) attestedCredential.getCredentialPublicKey();
		assertThat(key.getKeyType(), equalTo(KeyType.EC2));
		assertThat(key.getAlgorithm(), equalTo(Algorithm.ES256));
		assertThat(key.getCurve(), equalTo(Curve.secp256r1));
		assertThat(key.getX(), equalTo(Base64.getDecoder().decode("KAdACr2BYzM22clwTv3d/jS4SqV3BEwSu6SSkcu9kqQ=")));
		assertThat(key.getY(), equalTo(Base64.getDecoder().decode("zBh9ZybYDt8uzI1KA5juXxQ4Rn5gB0DAmDtg//e2F2s=")));

		AttestationStatement attStmt = attestation.getAttStmt();
		assertThat(attStmt.getSig(), equalTo(Base64.getDecoder().decode(
				"MEYCIQDB1sDlYOpmDEXQEJUsuJ00VApQ9dOIoOxMBSVBKHIKwgIhAJSNbyOPYr46sgZVfR8pnag8PjGIsuYI3RJT2ubL0s6s")));
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
		assertThat(cd.getChallenge(), equalTo("T0tONTZ4ZFVyTk5MNzA1OXN0QTJ0MnlvcDQ1MFdTTDI"));
		assertThat(cd.getOrigin(), equalTo("http://localhost:8080"));

		AuthenticatorData authData = response.getAuthenticatorData();

		assertThat(authData.getSignCount(), equalTo(104));
		assertThat(authData.isUserPresent(), equalTo(true));
		assertThat(authData.isUserVerified(), equalTo(false));
		assertThat(authData.hasAttestedcredentialData(), equalTo(false));
		assertThat(authData.hasExtensionData(), equalTo(false));
		assertThat(authData.getRpIdHash(),
				equalTo(Utils.decodeBase64url("SZYN5YgOjGh0NBcPZHZgW4_krrmihjLHmVzzuoMdl2M")));

		assertThat(response.getSignature(), equalTo(Base64.getDecoder().decode(
				"MEQCIBVF6MRCjeMhXF5m+PhbtPUFt5LgPg+w465LsTEeQ9dFAiAbOiZA8gbHKH4pVbdat0tVulx50CZW2XY5+EziXJvSdQ==")));
	}

}
