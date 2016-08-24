package org.ironrhino.core.security.action;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.ironrhino.core.event.EventPublisher;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.Captcha;
import org.ironrhino.core.metadata.Redirect;
import org.ironrhino.core.metadata.Scope;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.security.event.LoginEvent;
import org.ironrhino.core.spring.configuration.ResourcePresentConditional;
import org.ironrhino.core.spring.security.CredentialsNeedResetException;
import org.ironrhino.core.spring.security.DefaultAuthenticationSuccessHandler;
import org.ironrhino.core.spring.security.DefaultUsernamePasswordAuthenticationFilter;
import org.ironrhino.core.struts.BaseAction;
import org.ironrhino.core.util.ExceptionUtils;
import org.ironrhino.core.util.ReflectionUtils;
import org.ironrhino.core.util.RequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import com.opensymphony.xwork2.interceptor.annotations.InputConfig;

@AutoConfig
@ResourcePresentConditional("classpath*:resources/spring/applicationContext-security*.xml")
public class LoginAction extends BaseAction {

	public final static String COOKIE_NAME_LOGIN_USER = "U";

	private static final long serialVersionUID = 2783386542815083811L;

	protected static Logger logger = LoggerFactory.getLogger(LoginAction.class);

	protected String password;

	protected String username;

	@Value("${login.defaultTargetUrl:/}")
	protected String defaultTargetUrl;

	@Autowired
	protected transient UserDetailsService userDetailsService;

	@Autowired
	protected transient DefaultUsernamePasswordAuthenticationFilter usernamePasswordAuthenticationFilter;

	@Autowired
	private transient AuthenticationManager authenticationManager;

	@Autowired
	protected transient EventPublisher eventPublisher;

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

	@Override
	@Redirect
	@InputConfig(methodName = INPUT)
	@Captcha(threshold = 3)
	public String execute() throws Exception {
		HttpServletRequest request = ServletActionContext.getRequest();
		HttpServletResponse response = ServletActionContext.getResponse();
		Authentication authResult = null;
		try {
			UsernamePasswordAuthenticationToken attempt = new UsernamePasswordAuthenticationToken(username, password);
			WebAuthenticationDetailsSource wads = ReflectionUtils.getFieldValue(usernamePasswordAuthenticationFilter,
					"authenticationDetailsSource");
			attempt.setDetails(wads.buildDetails(request));
			authResult = authenticationManager.authenticate(attempt);
		} catch (InternalAuthenticationServiceException failed) {
			Throwable cause = failed.getCause();
			if (cause instanceof Exception) {
				throw (Exception) cause;
			} else {
				logger.error(failed.getMessage(), failed);
				addActionError(ExceptionUtils.getRootMessage(failed));
			}
		} catch (UsernameNotFoundException | DisabledException | LockedException | AccountExpiredException failed) {
			addFieldError("username", getText(failed.getClass().getName()));
		} catch (CredentialsNeedResetException failed) {
			addFieldError("password", getText(failed.getClass().getName()));
		} catch (BadCredentialsException failed) {
			addFieldError("password", getText(failed.getClass().getName()));
			captchaManager.addCaptachaCount(request);
			try {
				usernamePasswordAuthenticationFilter.unsuccess(request, response, failed);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		} catch (CredentialsExpiredException failed) {
			UserDetails ud = userDetailsService.loadUserByUsername(username);
			if (ud instanceof Persistable) {
				addActionMessage(getText(failed.getClass().getName()));
				authResult = new UsernamePasswordAuthenticationToken(ud, ud.getPassword(), ud.getAuthorities());
			} else {
				addFieldError("password", getText(failed.getClass().getName()));
				try {
					usernamePasswordAuthenticationFilter.unsuccess(request, response, failed);
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			}
		}
		if (authResult != null)
			try {
				usernamePasswordAuthenticationFilter.success(request, response, authResult);
				Object principal = authResult.getPrincipal();
				if (principal instanceof UserDetails)
					eventPublisher.publish(
							new LoginEvent(((UserDetails) principal).getUsername(), request.getRemoteAddr()),
							Scope.LOCAL);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		if (StringUtils.isBlank(targetUrl))
			targetUrl = defaultTargetUrl;
		return SUCCESS;
	}

	@Override
	public String input() {
		HttpServletRequest request = ServletActionContext.getRequest();
		username = RequestUtils.getCookieValue(request, DefaultAuthenticationSuccessHandler.COOKIE_NAME_LOGIN_USER);
		return SUCCESS;
	}

}
