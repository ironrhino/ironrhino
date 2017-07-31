package org.ironrhino.security.oauth.server.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.NaturalId;
import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.CaseInsensitive;
import org.ironrhino.core.metadata.Hidden;
import org.ironrhino.core.metadata.NotInCopy;
import org.ironrhino.core.metadata.Richtable;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.model.Attachmentable;
import org.ironrhino.core.model.BaseEntity;
import org.ironrhino.core.model.Enableable;
import org.ironrhino.core.security.role.UserRole;
import org.ironrhino.core.util.CodecUtils;
import org.ironrhino.security.model.User;

import lombok.Getter;
import lombok.Setter;

@AutoConfig
@Authorize(ifAllGranted = UserRole.ROLE_ADMINISTRATOR)
@Entity
@Table(name = "oauth_client")
@Richtable(order = "name asc", celleditable = false)
@Getter
@Setter
public class Client extends BaseEntity implements Enableable, Attachmentable {

	private static final long serialVersionUID = -7297737795748467475L;

	public static final String OAUTH_OOB = "urn:ietf:wg:oauth:2.0:oob";

	@UiConfig(cssClass = "input-xxlarge")
	@CaseInsensitive
	@NaturalId(mutable = true)
	@Column(nullable = false)
	private String name;

	@UiConfig(alias = "client_secret", cssClass = "input-xxlarge", width = "200px", excludedFromCriteria = true)
	@Column(nullable = false)
	private String secret = CodecUtils.nextId();

	@UiConfig(cssClass = "input-xxlarge", hiddenInList = @Hidden(true))
	private String redirectUri;

	@UiConfig(cssClass = "input-xxlarge", type = "textarea", hiddenInList = @Hidden(true))
	@Column(length = 4000)
	private String description;

	@NotInCopy
	@UiConfig(width = "150px")
	@ManyToOne(optional = false)
	@JoinColumn(name = "owner")
	private User owner;

	@UiConfig(width = "80px")
	private boolean enabled = true;

	@NotInCopy
	@UiConfig(hiddenInInput = @Hidden(true), hiddenInList = @Hidden(true))
	@Column(updatable = false)
	@CreationTimestamp
	private Date createDate;

	private List<String> attachments = new ArrayList<>(0);

	@UiConfig(displayOrder = 2, width = "200px", alias = "client_id")
	public String getClientId() {
		return getId();
	}

	@Override
	public String toString() {
		return this.name;
	}

	public boolean supportsRedirectUri(String redirectUri) {
		return StringUtils.isBlank(this.redirectUri) || this.redirectUri.equals(redirectUri);
	}

	@UiConfig(hidden = true)
	public boolean isNative() {
		return OAUTH_OOB.equals(redirectUri);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Client) {
			Client that = (Client) other;
			if (this.getId().equals(that.getId()) && this.getSecret().equals(that.getSecret())
					&& supportsRedirectUri(that.getRedirectUri()))
				return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return id != null ? id.hashCode() : -1;
	}

}
