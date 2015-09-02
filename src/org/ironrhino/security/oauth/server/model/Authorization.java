package org.ironrhino.security.oauth.server.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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
import org.ironrhino.core.spring.configuration.ResourcePresentConditional;
import org.ironrhino.core.util.CodecUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;

@AutoConfig
@Authorize(ifAllGranted = UserRole.ROLE_ADMINISTRATOR)
@Entity
@Table(name = "oauth_authorization")
@Richtable(order = "createDate desc", readonly = @Readonly(value = true, deletable = true) , bottomButtons = "<@btn view='create'/> <@btn action='delete' confirm=true/> <@btn class='reload'/> <@btn class='filter'/>")
@ResourcePresentConditional(value = "resources/spring/applicationContext-oauth.xml", negated = true)
public class Authorization extends BaseEntity {

	public static final int DEFAULT_LIFETIME = 3600;

	private static final long serialVersionUID = -559379341059695550L;

	@UiConfig(width = "180px", alias = "access_token")
	@CaseInsensitive
	@NaturalId(mutable = true)
	private String accessToken = CodecUtils.nextId();

	@Column(length = 32)
	@UiConfig(template = "<#if value?has_content><a href=\"<@url value='/oauth/client/view/${value}'/>\" rel=\"richtable\">${statics['org.ironrhino.core.util.ApplicationContextUtils'].getBean('oauthManager').findClientById(value)}</a></#if>")
	private String client;

	private String grantor;

	@UiConfig(hiddenInList = @Hidden(true) )
	private String scope;

	@UiConfig(hiddenInList = @Hidden(true) )
	@Column(unique = true)
	private String code;

	@UiConfig(width = "60px")
	private int lifetime = DEFAULT_LIFETIME;

	@UiConfig(hiddenInList = @Hidden(true) , alias = "refresh_token")
	@Column(unique = true)
	private String refreshToken;

	@UiConfig(hiddenInList = @Hidden(true) , alias = "response_type")
	@Column(nullable = false, length = 10)
	@Enumerated(EnumType.STRING)
	private ResponseType responseType = ResponseType.code;

	@UiConfig(width = "120px", alias = "grant_type")
	@Column(length = 20)
	@Enumerated(EnumType.STRING)
	private GrantType grantType = GrantType.authorization_code;

	@UiConfig(width = "130px")
	private String address;

	@NotInCopy
	@UiConfig(width = "130px")
	@Column(nullable = false, updatable = false)
	private Date createDate = new Date();

	@NotInCopy
	@UiConfig(width = "130px")
	@Column(nullable = false)
	private Date modifyDate = new Date();

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getClient() {
		return client;
	}

	public void setClient(String client) {
		this.client = client;
	}

	public String getGrantor() {
		return grantor;
	}

	public void setGrantor(String grantor) {
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
		return lifetime > 0 ? lifetime - (int) ((System.currentTimeMillis() - modifyDate.getTime()) / 1000)
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

	public ResponseType getResponseType() {
		return responseType;
	}

	public void setResponseType(ResponseType responseType) {
		this.responseType = responseType;
	}

	public GrantType getGrantType() {
		return grantType;
	}

	public void setGrantType(GrantType grantType) {
		this.grantType = grantType;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	@JsonIgnore
	public boolean isClientSide() {
		return ResponseType.token == responseType;
	}

}
