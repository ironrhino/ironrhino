package org.ironrhino.core.security.webauthn.domain.cose;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RSAKey extends Key {

	@JsonProperty("-1")
	private final byte[] n;

	@JsonProperty("-2")
	private final byte[] e;

	@JsonProperty("-3")
	private final byte[] d;

	@JsonProperty("-4")
	private final byte[] p;

	@JsonProperty("-5")
	private final byte[] q;

	@JsonProperty("-6")
	private final byte[] dP;

	@JsonProperty("-7")
	private final byte[] dQ;

	@JsonProperty("-8")
	private final byte[] qInv;

	@JsonCreator
	public RSAKey(@JsonProperty("2") byte[] keyId, @JsonProperty("3") Algorithm algorithm,
			@JsonProperty("4") List<KeyOperation> keyOps, @JsonProperty("-1") byte[] n, @JsonProperty("-2") byte[] e,
			@JsonProperty("-3") byte[] d, @JsonProperty("-4") byte[] p, @JsonProperty("-5") byte[] q,
			@JsonProperty("-6") byte[] dP, @JsonProperty("-7") byte[] dQ, @JsonProperty("-8") byte[] qInv) {
		super(keyId, algorithm, keyOps, null);
		this.n = n;
		this.e = e;
		this.d = d;
		this.p = p;
		this.q = q;
		this.dP = dP;
		this.dQ = dQ;
		this.qInv = qInv;
	}

	@Override
	public KeyType getKeyType() {
		return KeyType.RSA;
	}

	@Override
	public PublicKey getPublicKey() {
		RSAPublicKeySpec spec = new RSAPublicKeySpec(new BigInteger(1, getN()), new BigInteger(1, getE()));
		try {
			return keyFactory.generatePublic(spec);
		} catch (InvalidKeySpecException e) {
			throw new RuntimeException(e);
		}
	}

	private static final KeyFactory keyFactory;

	static {
		try {
			keyFactory = KeyFactory.getInstance("RSA");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

}
