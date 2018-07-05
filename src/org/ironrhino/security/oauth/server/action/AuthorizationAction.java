package org.ironrhino.security.oauth.server.action;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.ironrhino.core.struts.EntityAction;
import org.ironrhino.core.util.CodecUtils;
import org.ironrhino.security.oauth.server.enums.ResponseType;
import org.ironrhino.security.oauth.server.model.Authorization;
import org.ironrhino.security.oauth.server.service.OAuthManager;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.interceptor.annotations.InputConfig;
import com.opensymphony.xwork2.validator.annotations.RequiredStringValidator;
import com.opensymphony.xwork2.validator.annotations.Validations;
import com.opensymphony.xwork2.validator.annotations.ValidatorType;

import lombok.Getter;
import lombok.Setter;

public class AuthorizationAction extends EntityAction<String, Authorization> {

	private static final long serialVersionUID = 2920367147774798742L;

	@Autowired
	private OAuthManager oauthManager;

	@Getter
	@Setter
	private Authorization authorization;

	@InputConfig(resultName = "create")
	@Validations(requiredStrings = {
			// @RequiredStringValidator(type = ValidatorType.FIELD, fieldName =
			// "authorization.client", trim = true, key =
			// "validation.required"),
			@RequiredStringValidator(type = ValidatorType.FIELD, fieldName = "authorization.grantor", trim = true, key = "validation.required") })
	public String create() {
		if (authorization == null)
			return ACCESSDENIED;
		if (StringUtils.isBlank(authorization.getClient()))
			authorization.setClient(null);
		authorization.setRefreshToken(CodecUtils.nextId(32));
		authorization.setResponseType(ResponseType.token);
		authorization.setAddress(ServletActionContext.getRequest().getRemoteAddr());
		oauthManager.create(authorization);
		addActionMessage("create token: " + authorization.getAccessToken());
		return SUCCESS;
	}

}
