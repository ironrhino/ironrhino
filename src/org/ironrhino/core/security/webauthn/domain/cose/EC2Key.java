package org.ironrhino.core.security.webauthn.domain.cose;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
public class EC2Key extends Key {

	private EllipticCurve curve;

	private byte[] x;

	private byte[] y;

	private byte[] d;

	@JsonCreator
	public EC2Key(@JsonProperty("2") byte[] keyId, @JsonProperty("3") Algorithm algorithm,
			@JsonProperty("4") List<KeyOperation> keyOps, @JsonProperty("-1") EllipticCurve curve,
			@JsonProperty("-2") byte[] x, @JsonProperty("-3") byte[] y, @JsonProperty("-4") byte[] d) {
		super(keyId, algorithm, keyOps, null);
		this.curve = curve;
		this.x = x;
		this.y = y;
		this.d = d;
	}

	@Override
	public KeyType getKeyType() {
		return KeyType.EC2;
	}

	@Override
	public PublicKey getPublicKey() {
		ECPoint point = new ECPoint(new BigInteger(1, getX()), new BigInteger(1, getY()));
		ECPublicKeySpec spec = new ECPublicKeySpec(point, curve.getParameterSpec());
		try {
			return keyFactory.generatePublic(spec);
		} catch (InvalidKeySpecException e) {
			throw new RuntimeException(e);
		}
	}

	private static final KeyFactory keyFactory;

	static {
		try {
			keyFactory = KeyFactory.getInstance("EC");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

}
