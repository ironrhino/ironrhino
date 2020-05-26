package org.ironrhino.security.oauth.server.action;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.spring.security.DefaultUsernamePasswordAuthenticationFilter;
import org.ironrhino.core.struts.BaseAction;
import org.ironrhino.core.util.AuthzUtils;
import org.ironrhino.core.util.ExceptionUtils;
import org.ironrhino.security.oauth.server.enums.ResponseType;
import org.ironrhino.security.oauth.server.model.Authorization;
import org.ironrhino.security.oauth.server.model.Client;
import org.ironrhino.security.oauth.server.service.OAuthManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@AutoConfig
@Slf4j
public class AuthAction extends BaseAction {

	private static final long serialVersionUID = 8175470892708878896L;

	@Autowired
	private OAuthManager oauthManager;

	@Autowired
	private DefaultUsernamePasswordAuthenticationFilter usernamePasswordAuthenticationFilter;

	@Autowired
	private AuthenticationManager authenticationManager;

	@Getter
	@Setter
	private String username;

	@Getter
	@Setter
	private String password;

	@Getter
	@Setter
	private String client_id;

	@Getter
	@Setter
	private String redirect_uri;

	@Getter
	@Setter
	private String scope;

	@Getter
	@Setter
	private ResponseType response_type;

	@Getter
	@Setter
	private String state;

	@Getter
	@Setter
	private String approval_prompt;

	@Getter
	private Authorization authorization;

	@Getter
	private Client client;

	@Getter
	private boolean displayForNative;

	@Getter
	private boolean granted;

	@Getter
	private boolean denied;

	@Override
	public String execute() throws Exception {
		client = oauthManager.findClientById(client_id);
		if (client == null)
			throw new IllegalArgumentException("client_id_invalid");
		UserDetails grantor = AuthzUtils.getUserDetails();
		if (!"force".equals(approval_prompt) && grantor != null) {
			List<Authorization> auths = oauthManager.findAuthorizationsByGrantor(grantor.getUsername());
			for (Authorization auth : auths) {
				if (Objects.equals(auth.getClient(), client.getId())
						&& Objects.equals(auth.getResponseType(), response_type)
						&& Objects.equals(auth.getScope(), scope)) {
					authorization = auth;
					break;
				}
			}
			if (authorization != null) {
				authorization = oauthManager.reuse(authorization);
				return grant();
			}
		}
		authorization = oauthManager.generate(client, redirect_uri, scope, response_type);
		client = oauthManager.findClientById(authorization.getClient());
		displayForNative = client.isNative();
		setUid(authorization.getId());
		return SUCCESS;
	}

	public String grant() throws Exception {
		HttpServletRequest request = ServletActionContext.getRequest();
		HttpServletResponse response = ServletActionContext.getResponse();
		UserDetails grantor = AuthzUtils.getUserDetails();
		if (grantor == null) {
			try {
				Authentication authResult = authenticationManager
						.authenticate(new UsernamePasswordAuthenticationToken(username, password));
				if (authResult != null) {
					usernamePasswordAuthenticationFilter.success(request, response, authResult);
					try {
						request.changeSessionId();
					} catch (Exception e) {
						log.error(e.getMessage(), e);
					}
					grantor = (UserDetails) authResult.getPrincipal();
				}
			} catch (UsernameNotFoundException | DisabledException | LockedException | AccountExpiredException failed) {
				addFieldError("username", getText(failed.getClass().getName()));
				return HOME;
			} catch (BadCredentialsException | CredentialsExpiredException failed) {
				usernamePasswordAuthenticationFilter.unsuccess(request, response, failed);
				addFieldError("password", getText(failed.getClass().getName()));
				captchaManager.addCaptchaCount(request);
				return HOME;
			} catch (InternalAuthenticationServiceException failed) {
				log.error(failed.getMessage(), failed);
				addActionError(ExceptionUtils.getRootMessage(failed));
				return HOME;
			}
		}
		if (authorization == null)
			authorization = oauthManager.grant(getUid(), grantor.getUsername());
		client = oauthManager.findClientById(authorization.getClient());
		displayForNative = client.isNative();
		granted = true;
		if (displayForNative) {
			return HOME;
		} else {
			StringBuilder sb = new StringBuilder(redirect_uri);
			if (authorization.isClientSide()) {
				sb.append("#");
				sb.append("access_token=").append(authorization.getAccessToken());
				sb.append("&expires_in=").append(authorization.getExpiresIn());
			} else {
				sb.append(sb.indexOf("?") > 0 ? "&" : "?").append("code=").append(authorization.getCode());
			}
			if (StringUtils.isNotBlank(state))
				try {
					sb.append("&state=").append(URLEncoder.encode(state, "UTF-8"));
				} catch (UnsupportedEncodingException e) {
				}
			targetUrl = sb.toString();
		}
		return REDIRECT;
	}

	public String deny() {
		oauthManager.deny(getUid());
		denied = true;
		if (Client.OAUTH_OOB.equals(redirect_uri)) {
			displayForNative = true;
			return HOME;
		}
		StringBuilder sb = new StringBuilder(redirect_uri);
		sb.append(sb.indexOf("?") > 0 ? "&" : "?").append("error=access_denied");
		if (StringUtils.isNotBlank(state))
			try {
				sb.append("&state=").append(URLEncoder.encode(state, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
			}
		targetUrl = sb.toString();
		return REDIRECT;
	}

}
