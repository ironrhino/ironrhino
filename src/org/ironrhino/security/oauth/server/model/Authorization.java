package org.ironrhino.security.oauth.server.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.NaturalId;
import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.CaseInsensitive;
import org.ironrhino.core.metadata.Hidden;
import org.ironrhino.core.metadata.NotInCopy;
import org.ironrhino.core.metadata.Readonly;
import org.ironrhino.core.metadata.Richtable;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.model.BaseEntity;
import org.ironrhino.core.security.role.UserRole;
import org.ironrhino.core.util.CodecUtils;
import org.ironrhino.security.model.User;

@AutoConfig
@Authorize(ifAllGranted = UserRole.ROLE_ADMINISTRATOR)
@Entity
@Table(name = "oauth_authorization")
@Richtable(order = "createDate desc", readonly = @Readonly(value = true, deletable = true), bottomButtons = "<button type=\"button\" class=\"btn\" data-view=\"create\">${action.getText('create')}</button> <button type=\"button\" class=\"btn confirm\" data-action=\"delete\" data-shown=\"selected\" data-filterselector=\":not([data-deletable='false'])\">${action.getText('delete')}</button> <button type=\"button\" class=\"btn reload\">${action.getText('reload')}</button> <button type=\"button\" class=\"btn filter\">${action.getText('filter')}</button>")
public class Authorization extends BaseEntity {

	public static final int DEFAULT_LIFETIME = 3600;

	private static final long serialVersionUID = -559379341059695550L;

	@UiConfig(displayOrder = 1, width = "200px", alias = "access_token")
	@CaseInsensitive
	@NaturalId(mutable = true)
	private String accessToken = CodecUtils.nextId();

	@UiConfig(displayOrder = 2)
	@ManyToOne
	@JoinColumn(name = "client", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
	private Client client;

	@UiConfig(displayOrder = 3)
	@ManyToOne
	@JoinColumn(name = "grantor", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
	private User grantor;

	@UiConfig(displayOrder = 4, hiddenInList = @Hidden(true))
	private String scope;

	@UiConfig(displayOrder = 5, hiddenInList = @Hidden(true))
	@Column(unique = true)
	private String code;

	@UiConfig(displayOrder = 6, width = "100px")
	private int lifetime = DEFAULT_LIFETIME;

	@UiConfig(displayOrder = 7, hiddenInList = @Hidden(true), alias = "refresh_token")
	@Column(unique = true)
	private String refreshToken;

	@UiConfig(displayOrder = 8, width = "100px", alias = "response_type")
	@Column(nullable = false)
	private String responseType = "code";

	@NotInCopy
	@UiConfig(displayOrder = 9, width = "130px")
	@Column(nullable = false, updatable = false)
	private Date createDate = new Date();

	@NotInCopy
	@UiConfig(displayOrder = 10, width = "130px")
	@Column(nullable = false)
	private Date modifyDate = new Date();

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public Client getClient() {
		return client;
	}

	public void setClient(Client client) {
		this.client = client;
	}

	public User getGrantor() {
		return grantor;
	}

	public void setGrantor(User grantor) {
		this.grantor = grantor;
	}

	public String getScope() {
		return scope;
	}

	public void setScope(String scope) {
		this.scope = scope;
	}

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public Date getModifyDate() {
		return modifyDate;
	}

	public void setModifyDate(Date modifyDate) {
		this.modifyDate = modifyDate;
	}

	public int getLifetime() {
		return lifetime;
	}

	public void setLifetime(int lifetime) {
		this.lifetime = lifetime;
	}

	public int getExpiresIn() {
		return lifetime > 0 ? lifetime
				- (int) ((System.currentTimeMillis() - modifyDate.getTime()) / 1000)
				: Integer.MAX_VALUE;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getResponseType() {
		return responseType;
	}

	public void setResponseType(String responseType) {
		this.responseType = responseType;
	}

	public boolean isClientSide() {
		return "token".equals(responseType);
	}

}
