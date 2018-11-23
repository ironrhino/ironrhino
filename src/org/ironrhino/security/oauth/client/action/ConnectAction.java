package org.ironrhino.security.oauth.client.action;

import java.net.URLEncoder;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.ironrhino.common.support.SettingControl;
import org.ironrhino.core.event.EventPublisher;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.Scope;
import org.ironrhino.core.security.event.LoginEvent;
import org.ironrhino.core.struts.BaseAction;
import org.ironrhino.core.util.AuthzUtils;
import org.ironrhino.core.util.CodecUtils;
import org.ironrhino.core.util.RequestUtils;
import org.ironrhino.security.event.SignupEvent;
import org.ironrhino.security.model.User;
import org.ironrhino.security.oauth.client.model.Profile;
import org.ironrhino.security.oauth.client.service.OAuthProvider;
import org.ironrhino.security.oauth.client.service.OAuthProviderManager;
import org.ironrhino.security.service.UserManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@AutoConfig
public class ConnectAction extends BaseAction {

	private static final long serialVersionUID = 8175406892708878896L;

	public static final String SETTING_KEY_OAUTH_ENABLED = "oauth.enabled";

	private List<OAuthProvider> providers;

	@Autowired
	private UserManager userManager;

	@Autowired
	private EventPublisher eventPublisher;

	@Autowired
	private SettingControl settingControl;

	@Autowired
	private OAuthProviderManager oauthProviderManager;

	public List<OAuthProvider> getProviders() {
		return providers;
	}

	@Override
	public String execute() {
		if (!isEnabled())
			return ACCESSDENIED;
		HttpServletRequest request = ServletActionContext.getRequest();
		String id = getUid();
		if (StringUtils.isBlank(id)) {
			String error = request.getParameter("error");
			if (StringUtils.isNotBlank(error))
				addActionError(getText(error.replaceAll("_", ".")));
			providers = oauthProviderManager.getProviders();
			return SUCCESS;
		} else {
			try {
				OAuthProvider provider = oauthProviderManager.lookup(getUid());
				if (provider == null)
					return ACCESSDENIED;
				StringBuilder sb = new StringBuilder(RequestUtils.getBaseUrl(request));
				sb.append("/oauth/connect/auth/").append(provider.getName());
				if (StringUtils.isNotBlank(targetUrl))
					sb.append("?targetUrl=").append(URLEncoder.encode(targetUrl, "UTF-8"));
				targetUrl = provider.getAuthRedirectURL(request, sb.toString());
			} catch (Exception e) {
				e.printStackTrace();
			}
			return REDIRECT;
		}
	}

	public String auth() {
		if (!isEnabled())
			return ACCESSDENIED;
		HttpServletRequest request = ServletActionContext.getRequest();
		String error = request.getParameter("error");
		if (StringUtils.isNotBlank(error)) {
			targetUrl = "/oauth/connect?error=" + error;
			return REDIRECT;
		}
		try {
			OAuthProvider provider = oauthProviderManager.lookup(getUid());
			if (provider == null)
				return ACCESSDENIED;
			Profile p = provider.getProfile(request);
			if (p == null) {
				targetUrl = "/oauth/connect";
				return REDIRECT;
			}
			User user = AuthzUtils.getUserDetails();
			if (user == null) {
				String id = p.getUid();
				LoginEvent loginEvent;
				user = userManager.loadUserByUsername(id);
				if (user != null) {
					loginEvent = new LoginEvent(user.getUsername(), request.getRemoteAddr(), "oauth",
							provider.getName());
				} else {
					user = new User();
					user.setUsername(userManager.suggestUsername(id));
					user.setLegiblePassword(CodecUtils.randomString(10));
					if (StringUtils.isNotBlank(p.getEmail()))
						try {
							if (userManager.loadUserByUsername(p.getEmail()) == null)
								user.setEmail(p.getEmail());
						} catch (UsernameNotFoundException e1) {
							user.setEmail(p.getEmail());
						}
					user.setName(StringUtils.isNotBlank(p.getName()) ? p.getName() : p.getDisplayName());
					userManager.save(user);
					eventPublisher.publish(
							new SignupEvent(user.getUsername(), request.getRemoteAddr(), "oauth", provider.getName()),
							Scope.LOCAL);
					loginEvent = new LoginEvent(user.getUsername(), request.getRemoteAddr(), "oauth",
							provider.getName());
					loginEvent.setFirst(true);
				}
				AuthzUtils.autoLogin(user);
				eventPublisher.publish(loginEvent, Scope.LOCAL);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		// OAuth2.0
		String state = request.getParameter("state");
		if (StringUtils.isNotBlank(state))
			targetUrl = state;
		if (StringUtils.isBlank(targetUrl))
			targetUrl = "/";
		return REDIRECT;
	}

	private boolean isEnabled() {
		return settingControl.getBooleanValue(SETTING_KEY_OAUTH_ENABLED, false);
	}
}
