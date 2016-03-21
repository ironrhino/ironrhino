package org.ironrhino.security.oauth.server.action;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.ironrhino.core.event.EventPublisher;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.JsonConfig;
import org.ironrhino.core.metadata.Scope;
import org.ironrhino.core.security.role.UserRole;
import org.ironrhino.core.servlet.HttpErrorHandler;
import org.ironrhino.core.spring.security.CredentialsNeedResetException;
import org.ironrhino.core.spring.security.DefaultUsernamePasswordAuthenticationFilter;
import org.ironrhino.core.struts.BaseAction;
import org.ironrhino.core.util.AuthzUtils;
import org.ironrhino.core.util.ExceptionUtils;
import org.ironrhino.security.oauth.server.component.OAuthHandler;
import org.ironrhino.security.oauth.server.enums.GrantType;
import org.ironrhino.security.oauth.server.enums.ResponseType;
import org.ironrhino.security.oauth.server.event.AuthorizeEvent;
import org.ironrhino.security.oauth.server.model.Authorization;
import org.ironrhino.security.oauth.server.model.Client;
import org.ironrhino.security.oauth.server.service.OAuthManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@AutoConfig
public class Oauth2Action extends BaseAction {

	private static final long serialVersionUID = 8175470892708878896L;

	protected static Logger logger = LoggerFactory.getLogger(Oauth2Action.class);

	@Autowired
	protected transient EventPublisher eventPublisher;

	@Autowired
	private transient OAuthManager oauthManager;

	@Autowired(required = false)
	private HttpErrorHandler httpErrorHandler;

	@Autowired
	private transient UserDetailsService userDetailsService;

	@Autowired
	private transient DefaultUsernamePasswordAuthenticationFilter usernamePasswordAuthenticationFilter;

	@Autowired
	protected AuthenticationFailureHandler authenticationFailureHandler;

	@Autowired
	protected AuthenticationSuccessHandler authenticationSuccessHandler;

	@Autowired
	private transient AuthenticationManager authenticationManager;

	private String username;
	private String password;
	private String client_id;
	private String client_secret;
	private String redirect_uri;
	private String scope;
	private String code;
	private ResponseType response_type;
	private GrantType grant_type;
	private String state;
	private String access_token;
	private String refresh_token;
	private String token;
	private String approval_prompt;
	private Authorization authorization;
	private Client client;

	private Map<String, Serializable> tojson;
	private boolean displayForNative;
	private boolean granted;
	private boolean denied;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Map<String, Serializable> getTojson() {
		return tojson;
	}

	public Client getClient() {
		return client;
	}

	public Authorization getAuthorization() {
		return authorization;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getAccess_token() {
		return access_token;
	}

	public void setAccess_token(String access_token) {
		this.access_token = access_token;
	}

	public String getRefresh_token() {
		return refresh_token;
	}

	public void setRefresh_token(String refresh_token) {
		this.refresh_token = refresh_token;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getClient_id() {
		return client_id;
	}

	public void setClient_id(String client_id) {
		this.client_id = client_id;
	}

	public String getClient_secret() {
		return client_secret;
	}

	public void setClient_secret(String client_secret) {
		this.client_secret = client_secret;
	}

	public GrantType getGrant_type() {
		return grant_type;
	}

	public void setGrant_type(GrantType grant_type) {
		this.grant_type = grant_type;
	}

	public String getRedirect_uri() {
		return redirect_uri;
	}

	public void setRedirect_uri(String redirect_uri) {
		this.redirect_uri = redirect_uri;
	}

	public String getScope() {
		return scope;
	}

	public void setScope(String scope) {
		this.scope = scope;
	}

	public ResponseType getResponse_type() {
		return response_type;
	}

	public void setResponse_type(ResponseType response_type) {
		this.response_type = response_type;
	}

	public String getApproval_prompt() {
		return approval_prompt;
	}

	public void setApproval_prompt(String approval_prompt) {
		this.approval_prompt = approval_prompt;
	}

	public boolean isDisplayForNative() {
		return displayForNative;
	}

	public boolean isGranted() {
		return granted;
	}

	public boolean isDenied() {
		return denied;
	}

	@Override
	public String execute() {
		return SUCCESS;
	}

	public String auth() {
		HttpServletRequest request = ServletActionContext.getRequest();
		HttpServletResponse response = ServletActionContext.getResponse();
		try {
			client = oauthManager.findClientById(client_id);
			if (client == null)
				throw new IllegalArgumentException("CLIENT_ID_INVALID");
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
		} catch (Exception e) {
			if (httpErrorHandler != null
					&& httpErrorHandler.handle(request, response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage()))
				return NONE;
			try {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			return NONE;
		}
		return INPUT;
	}

	public String grant() {
		HttpServletRequest request = ServletActionContext.getRequest();
		HttpServletResponse response = ServletActionContext.getResponse();
		UserDetails grantor = AuthzUtils.getUserDetails();
		if (grantor == null) {
			try {
				Authentication authResult = authenticationManager
						.authenticate(new UsernamePasswordAuthenticationToken(username, password));
				if (authResult != null)
					try {
						usernamePasswordAuthenticationFilter.success(request, response, authResult);
						grantor = (UserDetails) authResult.getPrincipal();
					} catch (Exception e) {
						e.printStackTrace();
					}
			} catch (UsernameNotFoundException | DisabledException | LockedException | AccountExpiredException failed) {
				addFieldError("username", getText(failed.getClass().getName()));
				return INPUT;
			} catch (BadCredentialsException | CredentialsExpiredException | CredentialsNeedResetException failed) {
				addFieldError("password", getText(failed.getClass().getName()));
				captchaManager.addCaptachaThreshold(request);
				try {
					usernamePasswordAuthenticationFilter.unsuccess(request, response, failed);
				} catch (Exception e) {
					e.printStackTrace();
				}
				return INPUT;
			} catch (InternalAuthenticationServiceException failed) {
				logger.error(failed.getMessage(), failed);
				addActionError(ExceptionUtils.getRootMessage(failed));
				return INPUT;
			}
		}
		try {
			if (authorization == null)
				authorization = oauthManager.grant(getUid(), grantor.getUsername());
			client = oauthManager.findClientById(authorization.getClient());
			displayForNative = client.isNative();
			granted = true;
			if (displayForNative) {
				return INPUT;
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
						e.printStackTrace();
					}
				targetUrl = sb.toString();
			}
		} catch (Exception e) {
			if (httpErrorHandler != null
					&& httpErrorHandler.handle(request, response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage()))
				return NONE;
			try {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			return NONE;
		}
		return REDIRECT;
	}

	public String deny() {
		oauthManager.deny(getUid());
		denied = true;
		if (Client.OAUTH_OOB.equals(redirect_uri)) {
			displayForNative = true;
			return INPUT;
		}
		StringBuilder sb = new StringBuilder(redirect_uri);
		sb.append(sb.indexOf("?") > 0 ? "&" : "?").append("error=access_denied");
		if (StringUtils.isNotBlank(state))
			try {
				sb.append("&state=").append(URLEncoder.encode(state, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		targetUrl = sb.toString();
		return REDIRECT;
	}

	@JsonConfig(root = "tojson")
	public String token() {
		HttpServletRequest request = ServletActionContext.getRequest();
		HttpServletResponse response = ServletActionContext.getResponse();
		request.setAttribute(OAuthHandler.REQUEST_ATTRIBUTE_KEY_OAUTH_REQUEST, true);
		if (grant_type == GrantType.password) {
			client = oauthManager.findClientById(client_id);
			try {
				if (client == null)
					throw new IllegalArgumentException("CLIENT_ID_NOT_EXISTS");
				if (!client.getSecret().equals(client_secret))
					throw new IllegalArgumentException("CLIENT_SECRET_MISMATCH");
				if (client.getOwner() == null || !client.getOwner().getRoles().contains(UserRole.ROLE_ADMINISTRATOR))
					throw new IllegalArgumentException("CLIENT_UNAUTHORIZED");
				try {
					try {
						Authentication authResult = authenticationManager
								.authenticate(new UsernamePasswordAuthenticationToken(username, password));
						if (authResult != null)
							try {
								authenticationSuccessHandler.onAuthenticationSuccess(request, response, authResult);
							} catch (Exception e) {
								e.printStackTrace();
							}
					} catch (InternalAuthenticationServiceException failed) {
						throw new IllegalArgumentException(ExceptionUtils.getRootMessage(failed));
					} catch (AuthenticationException failed) {
						logger.error(failed.getMessage(), failed);
						try {
							authenticationFailureHandler.onAuthenticationFailure(request, response, failed);
						} catch (Exception e) {
							e.printStackTrace();
						}
						throw new IllegalArgumentException(getText(failed.getClass().getName()));
					}
					UserDetails u = userDetailsService.loadUserByUsername(username);
					authorization = oauthManager.grant(client, u.getUsername());
				} catch (UsernameNotFoundException e) {
					throw new IllegalArgumentException("USERNAME_NOT_EXISTS");
				}
			} catch (Exception e) {
				if (httpErrorHandler != null && httpErrorHandler.handle(request, response,
						HttpServletResponse.SC_BAD_REQUEST, e.getMessage()))
					return NONE;
				try {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
					return NONE;
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				return NONE;
			}
			tojson = new HashMap<>();
			tojson.put("access_token", authorization.getAccessToken());
			tojson.put("refresh_token", authorization.getRefreshToken());
			tojson.put("expires_in", authorization.getExpiresIn());
			eventPublisher.publish(
					new AuthorizeEvent(username, request.getRemoteAddr(), client.getName(), grant_type.name()),
					Scope.LOCAL);
			return JSON;
		} else if (grant_type == GrantType.client_credential) {
			client = new Client();
			client.setId(client_id);
			client.setSecret(client_secret);
			try {
				authorization = oauthManager.grant(client);
			} catch (Exception e) {
				if (httpErrorHandler != null && httpErrorHandler.handle(request, response,
						HttpServletResponse.SC_BAD_REQUEST, e.getMessage()))
					return NONE;
				try {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
					return NONE;
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				return NONE;
			}
			tojson = new HashMap<>();
			tojson.put("access_token", authorization.getAccessToken());
			tojson.put("refresh_token", authorization.getRefreshToken());
			tojson.put("expires_in", authorization.getExpiresIn());
			return JSON;
		} else if (grant_type == GrantType.refresh_token) {
			client = new Client();
			client.setId(client_id);
			client.setSecret(client_secret);
			try {
				authorization = oauthManager.refresh(client, refresh_token);
				tojson = new HashMap<>();
				tojson.put("access_token", authorization.getAccessToken());
				tojson.put("expires_in", authorization.getExpiresIn());
				tojson.put("refresh_token", authorization.getRefreshToken());
			} catch (Exception e) {
				if (httpErrorHandler != null && httpErrorHandler.handle(request, response,
						HttpServletResponse.SC_BAD_REQUEST, e.getMessage()))
					return NONE;
				try {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
					return NONE;
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				return NONE;
			}
			return JSON;
		} else {
			if (grant_type != GrantType.authorization_code) {
				String message = "grant_type must be authorization_code";
				if (httpErrorHandler != null
						&& httpErrorHandler.handle(request, response, HttpServletResponse.SC_BAD_REQUEST, message))
					return NONE;
				try {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				return NONE;
			}
			client = new Client();
			client.setId(client_id);
			client.setSecret(client_secret);
			client.setRedirectUri(redirect_uri);
			try {
				authorization = oauthManager.authenticate(code, client);
				tojson = new HashMap<>();
				tojson.put("access_token", authorization.getAccessToken());
				tojson.put("expires_in", authorization.getExpiresIn());
				tojson.put("refresh_token", authorization.getRefreshToken());
				eventPublisher.publish(
						new AuthorizeEvent(username, request.getRemoteAddr(), client.getName(), grant_type.name()),
						Scope.LOCAL);
			} catch (Exception e) {
				if (httpErrorHandler != null && httpErrorHandler.handle(request, response,
						HttpServletResponse.SC_BAD_REQUEST, e.getMessage()))
					return NONE;
				try {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				return NONE;
			}
			return JSON;
		}
	}

	@JsonConfig(root = "tojson")
	public String info() {
		HttpServletRequest request = ServletActionContext.getRequest();
		HttpServletResponse response = ServletActionContext.getResponse();
		if (access_token == null && token != null)
			access_token = token;
		tojson = new HashMap<>();
		authorization = oauthManager.retrieve(access_token);
		if (authorization == null) {
			if (httpErrorHandler != null
					&& httpErrorHandler.handle(request, response, HttpServletResponse.SC_UNAUTHORIZED, "invalid_token"))
				return NONE;
			tojson.put("error", "invalid_token");
		} else if (authorization.getExpiresIn() < 0) {
			if (httpErrorHandler != null
					&& httpErrorHandler.handle(request, response, HttpServletResponse.SC_UNAUTHORIZED, "expired_token"))
				return NONE;
			tojson.put("error", "expired_token");
		} else {
			if (authorization.getClient() != null)
				tojson.put("client_id", authorization.getClient());
			if (authorization.getGrantor() != null)
				tojson.put("username", authorization.getGrantor());
			tojson.put("expires_in", authorization.getExpiresIn());
			if (authorization.getScope() != null)
				tojson.put("scope", authorization.getScope());
		}
		return JSON;
	}

	public String revoke() {
		if (access_token == null && token != null)
			access_token = token;
		boolean revoked = oauthManager.revoke(access_token);
		if (httpErrorHandler != null) {
			HttpServletRequest request = ServletActionContext.getRequest();
			HttpServletResponse response = ServletActionContext.getResponse();
			httpErrorHandler.handle(request, response,
					revoked ? HttpServletResponse.SC_OK : HttpServletResponse.SC_NOT_FOUND, null);
		}
		return NONE;
	}

}
