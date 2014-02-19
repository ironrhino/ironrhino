package org.ironrhino.security.oauth.server.action;

import org.springframework.beans.factory.annotation.Autowired;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.struts.EntityAction;
import org.ironrhino.security.oauth.server.model.Authorization;
import org.ironrhino.security.oauth.server.model.Client;
import org.ironrhino.security.oauth.server.service.OAuthManager;

import com.opensymphony.xwork2.interceptor.annotations.InputConfig;
import com.opensymphony.xwork2.validator.annotations.RequiredStringValidator;
import com.opensymphony.xwork2.validator.annotations.Validations;
import com.opensymphony.xwork2.validator.annotations.ValidatorType;

public class AuthorizationAction extends EntityAction<Authorization> {

	private static final long serialVersionUID = 2920367147774798742L;

	@Autowired
	private OAuthManager oauthManager;

	private Authorization authorization;

	public Authorization getAuthorization() {
		return authorization;
	}

	public void setAuthorization(Authorization authorization) {
		this.authorization = authorization;
	}

	@InputConfig(methodName = "inputcreate")
	@Validations(requiredStrings = {
	// @RequiredStringValidator(type = ValidatorType.FIELD, fieldName =
	// "authorization.client.id", trim = true, key = "validation.required"),
	@RequiredStringValidator(type = ValidatorType.FIELD, fieldName = "authorization.grantor.id", trim = true, key = "validation.required") })
	public String create() {
		if (authorization == null)
			return ACCESSDENIED;
		if (authorization.getClient() != null
				&& StringUtils.isBlank(authorization.getClient().getId()))
			authorization.setClient(null);
		oauthManager.create(authorization);
		addActionMessage(getText("operate.success") + ",token:  "
				+ authorization.getAccessToken());
		return SUCCESS;
	}

	public String inputcreate() {
		if (authorization == null)
			authorization = new Authorization();
		Client client = authorization.getClient();
		if (client != null) {
			client = getEntityManager(Client.class).get(client.getId());
			authorization.setClient(client);
			if (client.getOwner() != null)
				authorization.setGrantor(client.getOwner());
		}
		return "create";
	}

}
