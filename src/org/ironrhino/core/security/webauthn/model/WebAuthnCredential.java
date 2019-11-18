package org.ironrhino.core.security.webauthn.model;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.Hidden;
import org.ironrhino.core.metadata.Readonly;
import org.ironrhino.core.metadata.Richtable;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.security.webauthn.domain.StoredCredential;
import org.ironrhino.core.security.webauthn.domain.cose.Key;
import org.ironrhino.core.security.webauthn.internal.Utils;
import org.ironrhino.core.spring.configuration.ApplicationContextPropertiesConditional;

import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@ApplicationContextPropertiesConditional(key = "webAuthn.enabled", value = "true")
@AutoConfig
@Entity
@Table(name = "webauthn_credential", indexes = @Index(columnList = "username"))
@Richtable(order = "createDate desc", readonly = @Readonly(value = true, deletable = true), showQueryForm = true, bottomButtons = "<@btn view='create'/> <@btn action='delete' confirm=true/> <@btn class='reload'/> <@btn class='filter'/>")
@Getter
@Setter
@NoArgsConstructor
public class WebAuthnCredential implements Persistable<String> {

	private static final long serialVersionUID = 8209157299970741136L;

	@Id
	private String id;

	@UiConfig(width = "180px")
	private String aaguid;

	@UiConfig(hiddenInList = @Hidden(true), viewTemplate = "<code class=\"block json\">${value}</code>", excludedFromCriteria = true)
	@Column(length = 4000)
	private String publicKey;

	@UiConfig(width = "120px")
	private String username;

	@UiConfig(width = "80px")
	private int signCount;

	@UiConfig(width = "100px")
	private String deviceName = "Unknown";

	@UiConfig(width = "160px")
	private LocalDateTime createDate = LocalDateTime.now();

	public String getCredentialId() {
		return id;
	}

	public WebAuthnCredential(StoredCredential c) {
		this.id = Utils.encodeBase64url(c.getCredentialId());
		this.aaguid = Utils.encodeBase64url(c.getAaguid());
		try {
			this.publicKey = Utils.JSON_OBJECTMAPPER.writeValueAsString(c.getCredentialPublicKey());
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
		this.signCount = c.getSignCount();
		this.username = c.getUsername();
	}

	public StoredCredential toStoredCredential() {
		try {
			return new StoredCredential(Utils.decodeBase64url(id), Utils.decodeBase64url(aaguid),
					Utils.JSON_OBJECTMAPPER.readValue(publicKey, Key.class), username, signCount);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
