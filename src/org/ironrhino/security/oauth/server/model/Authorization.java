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
import org.ironrhino.security.oauth.server.enums.GrantType;
import org.ironrhino.security.oauth.server.enums.ResponseType;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@AutoConfig
@Authorize(ifAllGranted = UserRole.ROLE_ADMINISTRATOR)
@Entity
@Table(name = "oauth_authorization")
@Richtable(order = "createDate desc", readonly = @Readonly(value = true, deletable = true), bottomButtons = "<@btn view='create'/> <@btn action='delete' confirm=true/> <@btn class='reload'/> <@btn class='filter'/>")
@ResourcePresentConditional(value = "resources/spring/applicationContext-oauth.xml", negated = true)
@Getter
@Setter
public class Authorization extends BaseEntity {

	public static final int LIFETIME_FOR_KICKED = -1;

	public static final int LIFETIME_FOR_PERMANENT = 0;

	private static final long serialVersionUID = -559379341059695550L;

	@UiConfig(width = "250px", alias = "access_token")
	@CaseInsensitive
	@NaturalId(mutable = true)
	private String accessToken = CodecUtils.nextId(32);

	@Column(length = 32)
	@UiConfig(template = "<#if value?has_content><a href=\"<@url value='/oauth/client/view/${value}'/>\" rel=\"richtable\">${beans['oauthManager'].findClientById(value)}</a></#if>")
	private String client;

	private String grantor;

	@UiConfig(hiddenInList = @Hidden(true))
	private String scope;

	@UiConfig(hiddenInList = @Hidden(true))
	@Column(unique = true)
	private String code;

	@UiConfig(width = "60px")
	private int lifetime;

	@UiConfig(hiddenInList = @Hidden(true), alias = "refresh_token")
	@Column(unique = true)
	private String refreshToken;

	@UiConfig(hiddenInList = @Hidden(true), alias = "response_type")
	@Column(nullable = false, length = 10)
	@Enumerated(EnumType.STRING)
	private ResponseType responseType = ResponseType.code;

	@UiConfig(width = "120px", alias = "grant_type")
	@Column(length = 20)
	@Enumerated(EnumType.STRING)
	private GrantType grantType = GrantType.authorization_code;

	@UiConfig(width = "130px")
	private String address;

	@UiConfig(hiddenInList = @Hidden(true))
	private String deviceId;

	@UiConfig(hiddenInList = @Hidden(true))
	private String deviceName;

	@NotInCopy
	@UiConfig(width = "130px")
	@Column(nullable = false, updatable = false)
	private Date createDate = new Date();

	@NotInCopy
	@UiConfig(width = "130px")
	@Column(nullable = false)
	private Date modifyDate = new Date();

	public int getExpiresIn() {
		if (lifetime == LIFETIME_FOR_KICKED)
			return 0;
		if (lifetime == LIFETIME_FOR_PERMANENT)
			return Integer.MAX_VALUE;
		return lifetime - (int) ((System.currentTimeMillis() - modifyDate.getTime()) / 1000);
	}

	@JsonIgnore
	public boolean isClientSide() {
		return ResponseType.token == responseType;
	}

	public boolean isKicked() {
		return lifetime == LIFETIME_FOR_KICKED;
	}

	public void markAsKicked() {
		lifetime = LIFETIME_FOR_KICKED;
	}

}
