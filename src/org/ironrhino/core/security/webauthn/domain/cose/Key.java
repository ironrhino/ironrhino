package org.ironrhino.core.security.webauthn.domain.cose;

import java.security.PublicKey;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.AllArgsConstructor;
import lombok.Data;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "1", include = JsonTypeInfo.As.EXISTING_PROPERTY)
@JsonSubTypes({ @JsonSubTypes.Type(value = EC2Key.class, name = "2"),
		@JsonSubTypes.Type(value = RSAKey.class, name = "3") })
@Data
@AllArgsConstructor
public abstract class Key {

	@JsonProperty("1")
	public abstract KeyType getKeyType();

	@JsonProperty("2")
	private final byte[] keyId;

	@JsonProperty("3")
	private final Algorithm algorithm;

	@JsonProperty("4")
	private final List<KeyOperation> keyOps;

	@JsonProperty("5")
	private final byte[] baseIV;

	@JsonIgnore
	public abstract PublicKey getPublicKey();

}
