package org.ironrhino.core.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Getter;

@Component
@Getter
public class SecurityConfig {

	@Value("${globalCookie:false}")
	private boolean globalCookie;

	@Value("${ssoServerBase:}")
	private String ssoServerBase;

	@Value("${login.defaultTargetUrl:/}")
	protected String loginDefaultTargetUrl;

	@Value("${password.entryPoint:/password}")
	private String passwordEntryPoint;

}
