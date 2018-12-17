package org.ironrhino.core.security.action;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.Captcha;
import org.ironrhino.core.metadata.Redirect;
import org.ironrhino.core.security.otp.TotpVerificationCodeChecker;
import org.ironrhino.core.security.role.UserRole;
import org.ironrhino.core.spring.configuration.ApplicationContextPropertiesConditional;
import org.ironrhino.core.struts.BaseAction;
import org.ironrhino.core.util.AppInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.opensymphony.xwork2.interceptor.annotations.InputConfig;

import lombok.Getter;
import lombok.Setter;

@Authorize(ifAnyGranted = UserRole.ROLE_ADMINISTRATOR)
@AutoConfig
@ApplicationContextPropertiesConditional(key = "totp.enabled", value = "true")
public class TotpAction extends BaseAction {

	private static final long serialVersionUID = 1440897497588169105L;

	@Autowired
	private UserDetailsService userDetailsService;

	@Autowired
	private TotpVerificationCodeChecker totpVerificationCodeChecker;

	@Getter
	@Setter
	@NotBlank
	protected String username;

	@Getter
	private String totpUri;

	@Override
	@Valid
	@Redirect
	@InputConfig(resultName = SUCCESS)
	@Captcha(threshold = 3)
	public String execute() throws Exception {
		try {
			totpUri = totpVerificationCodeChecker.of(userDetailsService.loadUserByUsername(username)).uri(username,
					getText(AppInfo.getAppName()));
		} catch (UsernameNotFoundException e) {
			addFieldError("username", getText("not.found"));
		}
		return SUCCESS;
	}

}
