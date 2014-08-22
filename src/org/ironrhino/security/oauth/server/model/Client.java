package org.ironrhino.security.oauth.server.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.NaturalId;
import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.CaseInsensitive;
import org.ironrhino.core.metadata.Hidden;
import org.ironrhino.core.metadata.NotInCopy;
import org.ironrhino.core.metadata.Richtable;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.model.BaseEntity;
import org.ironrhino.core.model.Enableable;
import org.ironrhino.core.security.role.UserRole;
import org.ironrhino.core.util.CodecUtils;
import org.ironrhino.security.model.User;

@AutoConfig
@Authorize(ifAllGranted = UserRole.ROLE_ADMINISTRATOR)
@Entity
@Table(name = "oauth_client")
@Richtable(order = "name asc", celleditable = false)
public class Client extends BaseEntity implements Enableable {

	private static final long serialVersionUID = -7297737795748467475L;

	public static final String OAUTH_OOB = "urn:ietf:wg:oauth:2.0:oob";

	@UiConfig(displayOrder = 1, cssClass = "span4")
	@CaseInsensitive
	@NaturalId(mutable = true)
	@Column(nullable = false)
	private String name;

	@UiConfig(displayOrder = 3, alias = "clientSecret", cssClass = "span4", width = "200px", excludedFromCriteria = true)
	@Column(nullable = false)
	private String secret = CodecUtils.nextId();

	@UiConfig(displayOrder = 4, cssClass = "span4", hiddenInList = @Hidden(true))
	private String redirectUri;

	@UiConfig(displayOrder = 5, cssClass = "span4", type = "textarea", hiddenInList = @Hidden(true))
	@Column(length = 4000)
	private String description;

	@NotInCopy
	@UiConfig(displayOrder = 6, width = "150px")
	@ManyToOne(optional = false)
	@JoinColumn(name = "owner", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
	private User owner;

	@UiConfig(displayOrder = 7, width = "80px")
	private boolean enabled = true;

	@NotInCopy
	@UiConfig(displayOrder = 8, hiddenInInput = @Hidden(true), hiddenInList = @Hidden(true))
	@Column(updatable = false)
	private Date createDate = new Date();

	@NotInCopy
	@UiConfig(displayOrder = 9, hiddenInInput = @Hidden(true), hiddenInList = @Hidden(true))
	@Column(insertable = false)
	private Date modifyDate;

	@UiConfig(displayOrder = 2, width = "200px")
	public String getClientId() {
		return getId();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public String getRedirectUri() {
		return redirectUri;
	}

	public void setRedirectUri(String redirectUri) {
		this.redirectUri = redirectUri;
	}

	public User getOwner() {
		return owner;
	}

	public void setOwner(User owner) {
		this.owner = owner;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Date getModifyDate() {
		return modifyDate;
	}

	public void setModifyDate(Date modifyDate) {
		this.modifyDate = modifyDate;
	}

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	@Override
	public String toString() {
		return this.name;
	}

	public boolean supportsRedirectUri(String redirectUri) {
		return StringUtils.isBlank(this.redirectUri)
				|| this.redirectUri.equals(redirectUri);
	}

	@UiConfig(hidden = true)
	public boolean isNative() {
		return OAUTH_OOB.equals(redirectUri);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Client) {
			Client that = (Client) other;
			if (this.getId().equals(that.getId())
					&& this.getSecret().equals(that.getSecret())
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
