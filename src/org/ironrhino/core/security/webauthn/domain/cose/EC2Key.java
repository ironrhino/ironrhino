package org.ironrhino.core.security.webauthn.domain.cose;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
public class EC2Key extends Key {

	@JsonProperty("-1")
	private Curve curve;

	@JsonProperty("-2")
	private byte[] x;

	@JsonProperty("-3")
	private byte[] y;

	@JsonProperty("-4")
	private byte[] d;

	public EC2Key(byte[] keyId, Algorithm algorithm, List<KeyOperation> keyOps, byte[] baseIV, Curve curve, byte[] x,
			byte[] y, byte[] d) {
		super(keyId, algorithm, keyOps, baseIV);
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
