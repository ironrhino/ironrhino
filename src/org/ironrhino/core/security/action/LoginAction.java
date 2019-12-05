package org.ironrhino.core.security.action;

import static org.ironrhino.core.security.action.PasswordAction.DEFAULT_VALUE_PASSWORD_ENTRY_POINT;
import static org.ironrhino.core.security.action.PasswordAction.KEY_PASSWORD_ENTRY_POINT;

import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.ironrhino.core.event.EventPublisher;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.Captcha;
import org.ironrhino.core.metadata.JsonConfig;
import org.ironrhino.core.metadata.Redirect;
import org.ironrhino.core.metadata.Scope;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.security.event.LoginEvent;
import org.ironrhino.core.security.verfication.ReceiverNotFoundException;
import org.ironrhino.core.security.verfication.VerificationManager;
import org.ironrhino.core.spring.security.DefaultAuthenticationSuccessHandler;
import org.ironrhino.core.spring.security.DefaultUsernamePasswordAuthenticationFilter;
import org.ironrhino.core.spring.security.SpringSecurityEnabled;
import org.ironrhino.core.spring.security.VerificationCodeRequirement;
import org.ironrhino.core.spring.security.VerificationCodeRequirementService;
import org.ironrhino.core.spring.security.WrongVerificationCodeException;
import org.ironrhino.core.struts.BaseAction;
import org.ironrhino.core.util.AuthzUtils;
import org.ironrhino.core.util.ExceptionUtils;
import org.ironrhino.core.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AccountExpiredException;
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
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;

import com.opensymphony.xwork2.interceptor.annotations.InputConfig;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@AutoConfig
@SpringSecurityEnabled
@Slf4j
public class LoginAction extends BaseAction {

	public final static String KEY_LOGIN_DEFAULT_TARGET_URL = "login.defaultTargetUrl";

	public final static String DEFAULT_VALUE_LOGIN_DEFAULT_TARGET_URL = "/";

	public final static String COOKIE_NAME_LOGIN_USER = "U";

	private static final long serialVersionUID = 2783386542815083811L;

	@Getter
	@Setter
	protected String password;

	@Getter
	@Setter
	@NotBlank
	protected String username;

	@Getter
	protected VerificationCodeRequirement verificationCodeRequirement;

	@Value("${" + KEY_LOGIN_DEFAULT_TARGET_URL + ":" + DEFAULT_VALUE_LOGIN_DEFAULT_TARGET_URL + "}")
	protected String defaultTargetUrl;

	@Value("${" + KEY_PASSWORD_ENTRY_POINT + ":" + DEFAULT_VALUE_PASSWORD_ENTRY_POINT + "}")
	private String passwordEntryPoint;

	@Autowired
	protected UserDetailsService userDetailsService;

	@Autowired
	protected DefaultUsernamePasswordAuthenticationFilter usernamePasswordAuthenticationFilter;

	@Autowired
	protected SessionAuthenticationStrategy sessionAuthenticationStrategy;

	@Autowired
	protected EventPublisher eventPublisher;

	@Autowired(required = false)
	@Getter
	protected VerificationManager verificationManager;

	@Autowired
	protected VerificationCodeRequirementService verificationCodeRequirementService;

	@Override
	@Valid
	@Redirect
	@InputConfig(methodName = INPUT)
	@Captcha(threshold = 3)
	public String execute() throws Exception {
		HttpServletRequest request = ServletActionContext.getRequest();
		HttpServletResponse response = ServletActionContext.getResponse();
		Authentication authResult = null;
		boolean credentialsExpired = false;
		try {
			UsernamePasswordAuthenticationToken attempt = new UsernamePasswordAuthenticationToken(username, password);
			attempt.setDetails(authenticationDetailsSource.buildDetails(request));
			authResult = authenticationManager.authenticate(attempt);
			sessionAuthenticationStrategy.onAuthentication(authResult, request, response);
		} catch (InternalAuthenticationServiceException failed) {
			Throwable cause = failed.getCause();
			if (cause instanceof Exception) {
				throw (Exception) cause;
			} else {
				log.error(failed.getMessage(), failed);
				addActionError(ExceptionUtils.getRootMessage(failed));
			}
		} catch (UsernameNotFoundException | DisabledException | LockedException | AccountExpiredException
				| ReceiverNotFoundException failed) {
			usernamePasswordAuthenticationFilter.unsuccess(request, response, failed);
			addFieldError("username", getText(failed.getClass().getName()));
		} catch (BadCredentialsException failed) {
			usernamePasswordAuthenticationFilter.unsuccess(request, response, failed);
			addFieldError("password", getText(failed.getClass().getName()));
			if (captchaManager != null) {
				captchaManager.addCaptchaCount(request);
			}
		} catch (CredentialsExpiredException failed) {
			UserDetails ud = userDetailsService.loadUserByUsername(username);
			if (ud instanceof Persistable) {
				addActionMessage(getText(failed.getClass().getName()));
				authResult = new UsernamePasswordAuthenticationToken(ud, ud.getPassword(), ud.getAuthorities());
				credentialsExpired = true;
			} else {
				usernamePasswordAuthenticationFilter.unsuccess(request, response, failed);
				addFieldError("password", getText(failed.getClass().getName()));
			}
		} catch (WrongVerificationCodeException failed) {
			usernamePasswordAuthenticationFilter.unsuccess(request, response, failed);
			addFieldError("verificationCode", getText(failed.getClass().getName()));
		}
		if (authResult != null) {
			try {
				usernamePasswordAuthenticationFilter.success(request, response, authResult);
				Object principal = authResult.getPrincipal();
				if (principal instanceof UserDetails)
					eventPublisher.publish(
							new LoginEvent(((UserDetails) principal).getUsername(), request.getRemoteAddr()),
							Scope.LOCAL);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
			if (credentialsExpired) {
				String s = passwordEntryPoint;
				if (StringUtils.isNotBlank(targetUrl))
					s += "?targetUrl=" + URLEncoder.encode(targetUrl, "UTF-8");
				targetUrl = s;
			} else if (StringUtils.isBlank(targetUrl)) {
				targetUrl = defaultTargetUrl;
			}
		}
		return REDIRECT;
	}

	@Override
	public String input() {
		HttpServletRequest request = ServletActionContext.getRequest();
		if (StringUtils.isNotBlank(targetUrl) && !RequestUtils.isSameOrigin(request, targetUrl))
			targetUrl = defaultTargetUrl;
		if (username == null)
			username = RequestUtils.getCookieValue(request, DefaultAuthenticationSuccessHandler.COOKIE_NAME_LOGIN_USER);
		String referer = request.getHeader("Referer");
		if (isAjax() && (referer == null || !referer.startsWith(RequestUtils.getBaseUrl(request) + "/login")))
			ServletActionContext.getResponse().setHeader(Redirect.RESPONSE_HEADER_NAME, targetUrl);
		return SUCCESS;
	}

	public String sendVerificationCode() {
		if (verificationManager == null)
			return NONE;
		String method = ServletActionContext.getRequest().getMethod();
		if (!method.equalsIgnoreCase("POST")) {
			addActionError(getText("validation.error"));
			return JSON;
		}
		if (StringUtils.isBlank(username))
			username = AuthzUtils.getUsername();
		if (verificationManager != null && StringUtils.isNotBlank(username)) {
			try {
				verificationManager.send(username);
				setActionSuccessMessage(getText("send.success"));
			} catch (AuthenticationException e) {
				addFieldError("username", getText(e.getClass().getName()));
			}
		}
		return JSON;
	}

	@JsonConfig(root = "verificationCodeRequirement")
	public String verificationCodeRequirement() {
		if (StringUtils.isBlank(username))
			username = AuthzUtils.getUsername();
		verificationCodeRequirement = verificationCodeRequirementService.getVerificationRequirement(username);
		return JSON;
	}

}
